/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
package org.pgptool.gui.app;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.pgptool.gui.autoupdate.impl.NewVersionCheckerGitHubImpl;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.tools.osnative.OsNativeApiResolver;
import org.pgptool.gui.tools.singleinstance.PrimaryInstanceListener;
import org.pgptool.gui.tools.singleinstance.SingleInstance;
import org.pgptool.gui.tools.singleinstance.SingleInstanceFileBasedImpl;
import org.pgptool.gui.ui.mainframe.MainFrameView;
import org.pgptool.gui.ui.root.RootPm;
import org.pgptool.gui.ui.tools.BindingContextFactoryImpl;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.api.UsageLoggerNoOpImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.skarpushin.swingpm.tools.SwingPmSettings;
import ru.skarpushin.swingpm.tools.edt.Edt;

public class EntryPoint {
	public static Logger log = Logger.getLogger(EntryPoint.class);
	public static EntryPoint INSTANCE;
	public static UsageLogger usageLoggerStatic = new UsageLoggerNoOpImpl();

	private static AbstractApplicationContext currentApplicationContext;
	private RootPm rootPm;
	private static SingleInstance singleInstance;
	private static Queue<String[]> postponedArgsFromSecondaryInstances = new ArrayDeque<>();
	public static RootPm rootPmStatic;

	public static void main(String[] args) {
		DOMConfigurator.configure(EntryPoint.class.getClassLoader().getResource("pgptool-gui-log4j.xml"));
		log.info("EntryPoint first scream");

		SplashScreenView splashScreenView = null;
		try {
			args = OsNativeApiResolver.resolve().getCommandLineArguments(args);
			if (!isContinueStartupSequence(args)) {
				System.exit(0);
				return;
			}

			UiUtils.setLookAndFeel();
			splashScreenView = new SplashScreenView();
			SwingPmSettings.setBindingContextFactory(new BindingContextFactoryImpl());

			// Startup application context
			String[] contextPaths = new String[] { "app-context.xml" };
			currentApplicationContext = new ClassPathXmlApplicationContext(contextPaths);
			log.debug("App context loaded");
			LocaleContextHolder.setLocale(new Locale(System.getProperty("user.language")));
			currentApplicationContext.registerShutdownHook();
			log.debug("Shutdown hook registered");

			// Now startup application logic
			EntryPoint entryPoint = currentApplicationContext.getBean(EntryPoint.class);
			usageLoggerStatic = currentApplicationContext.getBean(UsageLogger.class);
			log.debug("EntryPoint bean resolved");
			prefetchKeys();
			splashScreenView.close();
			splashScreenView = null;
			entryPoint.startUp(args);
			rootPmStatic = entryPoint.getRootPm();
			log.debug("RootPM been resolved");
			processPendingArgsIfAny(rootPmStatic);
		} catch (Throwable t) {
			log.error("Failed to startup application", t);
			reportAppInitFailureMessageToUser(determineWindowForGeneralFailure(splashScreenView), t);
			System.exit(-1);
		} finally {
			if (splashScreenView != null) {
				splashScreenView.close();
				splashScreenView = null;
			}
		}
	}

	private static Window determineWindowForGeneralFailure(SplashScreenView splashScreenView) {
		if (rootPmStatic != null) {
			return rootPmStatic.findMainFrameWindow();
		}

		if (splashScreenView != null) {
			return splashScreenView;
		}

		return null;
	}

	private static void processPendingArgsIfAny(RootPm rootPm) {
		while (!postponedArgsFromSecondaryInstances.isEmpty()) {
			String[] args = postponedArgsFromSecondaryInstances.poll();
			log.debug("Processing postponed args from secondary instance: " + Arrays.toString(args));
			rootPm.processCommandLine(args);
		}
	}

	private static void prefetchKeys() {
		new Thread("Prefetching keys") {
			@Override
			public void run() {
				try {
					KeyRingService keyRingService = (KeyRingService) currentApplicationContext
							.getBean("keyRingService");
					List<Key> keys = keyRingService.readKeys();
					log.info("Keys prefetched. Count " + keys.size());
				} catch (Throwable t) {
					log.error("Failed to prefetch keys", t);
				}
			};
		}.start();
	}

	private static boolean isContinueStartupSequence(String[] args) {
		singleInstance = new SingleInstanceFileBasedImpl("pgptool-si");
		if (!singleInstance.tryClaimPrimaryInstanceRole(primaryInstanceListener)) {
			boolean result = singleInstance.sendArgumentsToOtherInstance(args);
			if (result) {
				log.info(
						"Since this is a secondary instance args were forwarded to primary instance. This instance will now exit");
				return false;
			}
			log.info("Failed to forward args to primary instance. We'll have to process it ourself");
			// NOTE: Now we happen to be in indistinctive state. We're not a
			// primary instance and not secondary :-( But it feels like it's
			// better that way since our goal is to make sure user request is
			// fulfilled
			singleInstance = null;
		}
		return true;
	}

	private static PrimaryInstanceListener primaryInstanceListener = new PrimaryInstanceListener() {
		@Override
		public void handleArgsFromOtherInstance(String[] args) {
			if (rootPmStatic != null) {
				log.debug("Processing arguments from secondary instance: " + Arrays.toString(args));
				Edt.invokeOnEdtAsync(new Runnable() {
					@Override
					public void run() {
						rootPmStatic.processCommandLine(args);
					}
				});
			} else {
				log.debug("Postponing args processing from secondary instance: " + Arrays.toString(args));
				postponedArgsFromSecondaryInstances.offer(args);
			}
		}
	};

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
			log.debug("Context stopped, closed, destroyed");
		} catch (Throwable t) {
			log.error("Failed to tear down context", t);
		}
	}

	public RootPm getRootPm() {
		return rootPm;
	}

	@Autowired
	public void setRootPm(RootPm rootPm) {
		this.rootPm = rootPm;
	}

	private static void reportAppInitFailureMessageToUser(Window window, Throwable t) {
		String versionInfo = NewVersionCheckerGitHubImpl.getVerisonsInfo();
		String msg = ConsoleExceptionUtils.getAllMessages(t);
		msg += "\r\n" + versionInfo;
		UiUtils.messageBox(UiUtils.actionEvent(window, "reportAppInitFailureMessageToUser"), msg,
				"PGPTool startup failed" + versionInfo, JOptionPane.ERROR_MESSAGE);
	}

	public static void reportExceptionToUser(ActionEvent originAction, String errorMessageCode, Throwable cause,
			Object... messageArgs) {
		GenericException exc = new GenericException(errorMessageCode, cause, messageArgs);
		String versionInfo = NewVersionCheckerGitHubImpl.getVerisonsInfo();
		String msgs = ConsoleExceptionUtils.getAllMessages(exc);
		msgs += "\r\n" + versionInfo;
		UiUtils.messageBox(originAction, msgs, Messages.get("term.error") + versionInfo, JOptionPane.ERROR_MESSAGE);
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
