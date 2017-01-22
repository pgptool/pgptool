package org.pgpvault.gui.ui.root;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.apache.log4j.Logger;
import org.pgpvault.gui.app.EntryPoint;
import org.pgpvault.gui.app.MessageSeverity;
import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.tools.ConsoleExceptionUtils;
import org.pgpvault.gui.ui.about.AboutHost;
import org.pgpvault.gui.ui.about.AboutPm;
import org.pgpvault.gui.ui.about.AboutView;
import org.pgpvault.gui.ui.importcertificate.KeyImporterHost;
import org.pgpvault.gui.ui.importcertificate.KeyImporterPm;
import org.pgpvault.gui.ui.importcertificate.KeyImporterView;
import org.pgpvault.gui.ui.mainframe.MainFrameHost;
import org.pgpvault.gui.ui.mainframe.MainFramePm;
import org.pgpvault.gui.ui.mainframe.MainFrameView;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.eventbus.EventBus;

import ru.skarpushin.swingpm.tools.actions.LocalizedAction;

/**
 * Root presentation model
 * 
 * @author sergeyk
 * 
 */
public class RootPm implements ApplicationContextAware, InitializingBean {
	private static Logger log = Logger.getLogger(RootPm.class);

	@Autowired
	private EntryPoint entryPoint;
	private ApplicationContext applicationContext;
	private EventBus eventBus;
	private ConfigRepository configRepository;

	private MainFramePm mainFramePm;
	private MainFrameView mainFrameView;

	private AboutView aboutView;
	private AboutPm aboutPm;

	private KeyImporterPm keyImporterPm;
	private KeyImporterView keyImporterView;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		eventBus.register(this);
	}

	public void present() {
		try {
			openMainFrameWindow();
		} catch (Throwable t) {
			try {
				log.error("Failed to start application", t);
				EntryPoint.showMessageBox(ConsoleExceptionUtils.getAllMessages(t),
						Messages.get("exception.unexpected.failedToStartupApplication"), MessageSeverity.ERROR);
			} finally {
				// NOTE: We actually mean to exit application
				exitApplication(-1);
			}
		}
	}

	private void exitApplication(int statusCode) {
		entryPoint.tearDownContext();
		System.exit(statusCode);
	}

	@SuppressWarnings("serial")
	private final Action actionImportCertificate = new LocalizedAction("action.importPgpCertificate") {
		@Override
		public void actionPerformed(ActionEvent e) {
			openPgpCertificateImportWindow();
		}
	};

	private void openPgpCertificateImportWindow() {
		keyImporterPm = applicationContext.getBean(KeyImporterPm.class);
		if (!keyImporterPm.init(keyImporterHost)) {
			// This happens if user clicked cancel during first render of
			// "Browse dialog"
			keyImporterPm.detach();
			keyImporterPm = null;
			return;
		}

		if (keyImporterView == null) {
			keyImporterView = applicationContext.getBean(KeyImporterView.class);
		}
		keyImporterView.setPm(keyImporterPm);
		keyImporterView.renderTo(null);
	}

	private KeyImporterHost keyImporterHost = new KeyImporterHost() {
		@Override
		public void handleImporterFinished() {
			keyImporterView.unrender();
			keyImporterPm.detach();
			keyImporterPm = null;
		}
	};

	@SuppressWarnings("serial")
	private final Action actionShowAboutInfo = new LocalizedAction("term.aboutApp") {
		@Override
		public void actionPerformed(ActionEvent e) {
			openAboutWindow();
		}
	};

	public void openAboutWindow() {
		aboutPm = applicationContext.getBean(AboutPm.class);
		aboutPm.init(aboutHost);

		if (aboutView == null) {
			aboutView = applicationContext.getBean(AboutView.class);
		}
		aboutView.setPm(aboutPm);
		aboutView.renderTo(null);
	}

	private AboutHost aboutHost = new AboutHost() {
		@Override
		public void handleClose() {
			aboutView.unrender();
			aboutPm.detach();
			aboutPm = null;
		}
	};

	private MainFrameHost mainFrameHost = new MainFrameHost() {
		@Override
		public void handleExitApp() {
			tearDownConfigContext();
			exitApplication(0);
		}

		@Override
		public Action getActionShowAboutInfo() {
			return actionShowAboutInfo;
		}

		@Override
		public Action getActionImportCertificate() {
			return actionImportCertificate;
		}
	};

	private void openMainFrameWindow() {
		mainFramePm = applicationContext.getBean(MainFramePm.class);
		mainFramePm.init(mainFrameHost);
		MainFrameView view = getMainFrameView();
		view.setPm(mainFramePm);
		view.renderTo(null);
	}

	protected void tearDownConfigContext() {
		try {
			getMainFrameView().unrender();
			getMainFrameView().detach();

			mainFramePm.detach();
			mainFramePm = null;
		} catch (Throwable t) {
			log.error("Failed to gracefully close app", t);
			EntryPoint.reportExceptionToUser("exception.unexpected.failedToCloseConfig", t);
		}
	}

	private MainFrameView getMainFrameView() {
		if (mainFrameView == null) {
			mainFrameView = applicationContext.getBean(MainFrameView.class);
		}
		return mainFrameView;
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	@Autowired
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public ConfigRepository getConfigRepository() {
		return configRepository;
	}

	@Autowired
	public void setConfigRepository(ConfigRepository configRepository) {
		this.configRepository = configRepository;
	}

}
