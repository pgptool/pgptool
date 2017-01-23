package org.pgpvault.gui.app;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.pgpvault.gui.tools.ConsoleExceptionUtils;
import org.pgpvault.gui.ui.root.RootPm;
import org.pgpvault.gui.ui.tools.BindingContextFactoryImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import ru.skarpushin.swingpm.tools.SwingPmSettings;

public class EntryPoint implements InitializingBean {
	private static Logger log = Logger.getLogger(EntryPoint.class);
	public static EntryPoint INSTANCE;

	private static AbstractApplicationContext currentApplicationContext;
	private RootPm rootPm;

	public static void main(String[] args) {
		SplashScreenView splashScreenView = new SplashScreenView();
		SwingPmSettings.setBindingContextFactory(new BindingContextFactoryImpl());

		try {
			DOMConfigurator.configure(EntryPoint.class.getClassLoader().getResource("pgpvault-gui-log4j.xml"));
			log.info("EntryPoint first scream");

			// Init spring context
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				log.info("L&F set");

				// Startup application context
				String[] contextPaths = new String[] { "app-context.xml" };
				currentApplicationContext = new ClassPathXmlApplicationContext(contextPaths);
				log.info("App context loaded");
				LocaleContextHolder.setLocale(new Locale(System.getProperty("user.language")));
				currentApplicationContext.registerShutdownHook();
				log.info("Shutdown hook registered");

				// Now startup application logic
				EntryPoint entryPoint = currentApplicationContext.getBean(EntryPoint.class);
				entryPoint.startUp();
			} catch (Throwable t) {
				log.error("Failed to startup application", t);
				reportAppInitFailureMessageToUser(t);
				throw new RuntimeException("Application failed to start", t);
			}
		} finally {
			splashScreenView.close();
		}
	}

	private static void reportAppInitFailureMessageToUser(Throwable t) {
		String msg = ConsoleExceptionUtils.getAllMessages(t);
		showMessageBox(null, msg, "Failed to init application", JOptionPane.ERROR_MESSAGE);
	}

	public EntryPoint() {
		INSTANCE = this;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	private void startUp() {
		rootPm.present();
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

}
