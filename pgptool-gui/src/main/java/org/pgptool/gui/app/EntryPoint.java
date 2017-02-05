/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.pgptool.gui.app;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.tools.osnative.OsNativeApiResolver;
import org.pgptool.gui.tools.singleinstance.PrimaryInstanceListener;
import org.pgptool.gui.tools.singleinstance.SingleInstance;
import org.pgptool.gui.tools.singleinstance.SingleInstanceFileBasedImpl;
import org.pgptool.gui.ui.mainframe.MainFrameView;
import org.pgptool.gui.ui.root.RootPm;
import org.pgptool.gui.ui.tools.BindingContextFactoryImpl;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.skarpushin.swingpm.tools.SwingPmSettings;

public class EntryPoint {
	public static Logger log = Logger.getLogger(EntryPoint.class);
	public static EntryPoint INSTANCE;

	private static AbstractApplicationContext currentApplicationContext;
	private RootPm rootPm;
	private static SingleInstance singleInstance;
	private static RootPm rootPmStatic;

	public static void main(String[] args) {
		DOMConfigurator.configure(EntryPoint.class.getClassLoader().getResource("pgptool-gui-log4j.xml"));
		log.info("EntryPoint first scream");

		SplashScreenView splashScreenView = null;
		try {
			args = OsNativeApiResolver.resolve().getCommandLineArguments(args);
			if (!isPrimaryInstance(args)) {
				log.info(
						"Since this is a secondary instance args were forwarded to primary instance. This instance will now exit");
				System.exit(0);
				return;
			}

			UiUtils.setLookAndFeel();
			splashScreenView = new SplashScreenView();
			SwingPmSettings.setBindingContextFactory(new BindingContextFactoryImpl());

			// Startup application context
			String[] contextPaths = new String[] { "app-context.xml" };
			currentApplicationContext = new ClassPathXmlApplicationContext(contextPaths);
			log.info("App context loaded");
			LocaleContextHolder.setLocale(new Locale(System.getProperty("user.language")));
			currentApplicationContext.registerShutdownHook();
			log.info("Shutdown hook registered");

			// Now startup application logic
			EntryPoint entryPoint = currentApplicationContext.getBean(EntryPoint.class);
			prefetchKeys();
			splashScreenView.close();
			splashScreenView = null;
			entryPoint.startUp(args);
			rootPmStatic = entryPoint.getRootPm();
		} catch (Throwable t) {
			log.error("Failed to startup application", t);
			reportAppInitFailureMessageToUser(t);
			throw new RuntimeException("Application failed to start", t);
		} finally {
			if (splashScreenView != null) {
				splashScreenView.close();
				splashScreenView = null;
			}
		}
	}

	private static void prefetchKeys() {
		new Thread("Prefetching keys") {
			@SuppressWarnings("rawtypes")
			@Override
			public void run() {
				try {
					KeyRingService keyRingService = (KeyRingService) currentApplicationContext
							.getBean("keyRingService");
					List keys = keyRingService.readKeys();
					log.info("Keys prefetched. Count " + keys.size());
				} catch (Throwable t) {
					log.error("Failed to prefetch keys", t);
				}
			};
		}.start();
	}

	private static boolean isPrimaryInstance(String[] args) {
		singleInstance = new SingleInstanceFileBasedImpl("pgptool-si");
		if (!singleInstance.tryClaimPrimaryInstanceRole(primaryInstanceListener)) {
			singleInstance.sendArgumentsToOtherInstance(args);
			return false;
		}
		return true;
	}

	private static PrimaryInstanceListener primaryInstanceListener = new PrimaryInstanceListener() {
		@Override
		public void handleArgsFromOtherInstance(String[] args) {
			log.debug("Processing arguments from secondary instance: " + Arrays.toString(args));

			if (rootPmStatic != null) {
				rootPmStatic.processCommandLine(args);
			}
		}
	};

	private static void reportAppInitFailureMessageToUser(Throwable t) {
		String msg = ConsoleExceptionUtils.getAllMessages(t);
		showMessageBox(null, msg, "Failed to init application", JOptionPane.ERROR_MESSAGE);
	}

	public EntryPoint() {
		INSTANCE = this;
	}

	private void startUp(String[] args) {
		rootPm.present(args);
		log.debug("entryPoint.startUp() finished");
	}

	public void tearDownContext() {
		try {
			log.debug("Tearing down context");
			currentApplicationContext.stop();
			currentApplicationContext.close();
			currentApplicationContext.destroy();
			log.debug("Context stopped, closed, destroyed");
		} catch (Throwable t) {
			log.error("Failed to tear down context", t);
		}
	}

	public static void showMessageBox(String messageText, String messageTitle, MessageSeverity messageSeverity) {
		int messageType = JOptionPane.INFORMATION_MESSAGE;
		if (messageSeverity == MessageSeverity.ERROR) {
			messageType = JOptionPane.ERROR_MESSAGE;
		} else if (messageSeverity == MessageSeverity.WARNING) {
			messageType = JOptionPane.WARNING_MESSAGE;
		} else if (messageSeverity == MessageSeverity.INFO) {
			messageType = JOptionPane.INFORMATION_MESSAGE;
		}
		showMessageBox(null, messageText, messageTitle, messageType);
	}

	public static void showMessageBox(Component parent, String msg, String title, int messageType) {
		JTextArea textArea = new JTextArea(msg);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		textArea.setMargin(new Insets(5, 5, 5, 5));
		textArea.setFont(textArea.getFont().deriveFont(12f));
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setPreferredSize(new Dimension(500, 150));
		scrollPane.getViewport().setView(textArea);
		JOptionPane.showMessageDialog(parent, scrollPane, title, messageType);
	}

	public RootPm getRootPm() {
		return rootPm;
	}

	@Autowired
	public void setRootPm(RootPm rootPm) {
		this.rootPm = rootPm;
	}

	public static void reportExceptionToUser(String errorMessageCode, Throwable cause) {
		GenericException exc = new GenericException(errorMessageCode, cause);
		String msgs = ConsoleExceptionUtils.getAllMessages(exc);
		// String msgs = ErrorUtils.getAllMessages(exc);
		showMessageBox(null, msgs, Messages.get("term.error"), JOptionPane.ERROR_MESSAGE);
	}

	public ApplicationContext getApplicationContext() {
		return currentApplicationContext;
	}

	public static Icon loadImage(String fileName) {
		URL resource = MainFrameView.class.getResource(fileName);
		if (resource == null) {
			log.error("Image not found: " + fileName);
			return null;
		}
		return new ImageIcon(resource);
	}

}
