package org.pgpvault.gui.ui.root;

import static org.pgpvault.gui.app.Messages.text;

import java.awt.Window;
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
import org.pgpvault.gui.ui.decryptone.DecryptOneHost;
import org.pgpvault.gui.ui.decryptone.DecryptOnePm;
import org.pgpvault.gui.ui.decryptone.DecryptOneView;
import org.pgpvault.gui.ui.encryptone.EncryptOneHost;
import org.pgpvault.gui.ui.encryptone.EncryptOnePm;
import org.pgpvault.gui.ui.encryptone.EncryptOneView;
import org.pgpvault.gui.ui.importkey.KeyImporterHost;
import org.pgpvault.gui.ui.importkey.KeyImporterPm;
import org.pgpvault.gui.ui.importkey.KeyImporterView;
import org.pgpvault.gui.ui.keyslist.KeysListHost;
import org.pgpvault.gui.ui.keyslist.KeysListPm;
import org.pgpvault.gui.ui.keyslist.KeysListView;
import org.pgpvault.gui.ui.mainframe.MainFrameHost;
import org.pgpvault.gui.ui.mainframe.MainFramePm;
import org.pgpvault.gui.ui.mainframe.MainFrameView;
import org.pgpvault.gui.ui.tempfolderfordecrypted.TempFolderChooserPm;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.common.eventbus.EventBus;

import ru.skarpushin.swingpm.base.HasWindow;
import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.base.ViewBase;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.tools.edt.Edt;

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

	private TempFolderChooserPm tempFolderChooserPm;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		eventBus.register(this);
	}

	public void present(String[] commandLineArgs) {
		try {
			openMainFrameWindow();
			processCommandLine(commandLineArgs);
			// THINK: Perhaps we'd better not open main frame if we were invoked
			// just in order to encrypt/decrypt something
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

	/**
	 * 
	 * @param commandLineArgs
	 * @return true if command line command processed and ther is no need to
	 *         continue application run sequence
	 */
	public boolean processCommandLine(String[] commandLineArgs) {
		try {
			if (commandLineArgs == null || commandLineArgs.length == 0) {
				if (mainFrameView != null) {
					mainFrameView.bringToFront();
				}
				return false;
			}

			if (commandLineArgs.length > 1) {
				log.warn("As of now application is not designed to handle more than 1 input file.");
			}

			// NOTE: We'll need to learn how to handle more than 1 input file
			// AND MAKE SURE UX will not suffer.
			// https://github.com/skarpushin/pgpvault/issues/9
			for (int i = 0; i < 1 /* commandLineArgs.length */; i++) {
				String file = commandLineArgs[i];
				if (DecryptOnePm.isItLooksLikeYourSourceFile(file)) {
					openNewDecryptionWindow(file);
				} else if (EncryptOnePm.isItLooksLikeYourSourceFile(file)) {
					openNewEncryptionWindow(file);
				} else {
					EntryPoint.showMessageBox("Program argument cannot be handled: " + file, text("term.attention"),
							MessageSeverity.WARNING);
				}
			}
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser("error.failedToProcessCommandLine", t);
		}
		return false;
	}

	private void openNewDecryptionWindow(final String sourceFile) {
		Edt.invokeOnEdtAndWait(new Runnable() {
			@Override
			public void run() {
				// NOTE: Assuming it is blocking operation!
				// Remember: View is singleton here
				new DecryptionWindowOpener(sourceFile).actionToOpenWindow.actionPerformed(null);
			}
		});
	}

	private void openNewEncryptionWindow(final String sourceFile) {
		Edt.invokeOnEdtAndWait(new Runnable() {
			@Override
			public void run() {
				// NOTE: Assuming it is blocking operation!
				// Remember: View is singleton here
				new EncryptionWindowOpener(sourceFile).actionToOpenWindow.actionPerformed(null);
			}
		});
	}

	private void exitApplication(int statusCode) {
		entryPoint.tearDownContext();
		System.exit(statusCode);
	}

	private MainFrameHost mainFrameHost = new MainFrameHost() {
		@Override
		public void handleExitApp() {
			tearDownConfigContext();
			exitApplication(0);
		}

		@Override
		public Action getActionShowAboutInfo() {
			return aboutWindowHost.actionToOpenWindow;
		}

		@Override
		public Action getActionImportKey() {
			return importKeyWindowHost.actionToOpenWindow;
		}

		@Override
		public Action getActionShowKeysList() {
			return keysListWindowHost.actionToOpenWindow;
		}

		@Override
		public Action getActionForEncrypt() {
			return encryptionWindowHost.actionToOpenWindow;
		}

		@Override
		public Action getActionForDecrypt() {
			return decryptionWindowHost.actionToOpenWindow;
		}

		@Override
		public Action getActionChangeFolderForDecrypted() {
			return actionShowTempFolderChooser;
		}

		@Override
		public void openEncryptDialogFor(String decryptedFile) {
			new EncryptionWindowOpener(decryptedFile).actionToOpenWindow.actionPerformed(null);
		}
	};

	private void openMainFrameWindow() {
		mainFramePm = applicationContext.getBean(MainFramePm.class);
		mainFramePm.init(mainFrameHost);
		MainFrameView view = getMainFrameView();
		view.setPm(mainFramePm);
		view.renderTo(null);
	}

	@SuppressWarnings("serial")
	protected Action actionShowTempFolderChooser = new LocalizedAction("term.changeTempFolderForDecrypted") {
		@Override
		public void actionPerformed(ActionEvent e) {
			tempFolderChooserPm = applicationContext.getBean(TempFolderChooserPm.class);
			tempFolderChooserPm.present(mainFrameView == null ? null : mainFrameView.getWindow());
			tempFolderChooserPm = null;
		}
	};

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

	private DialogOpener<KeyImporterPm, KeyImporterView> importKeyWindowHost = new DialogOpener<KeyImporterPm, KeyImporterView>(
			KeyImporterPm.class, KeyImporterView.class, "action.importKey") {

		KeyImporterHost host = new KeyImporterHost() {
			@Override
			public void handleImporterFinished() {
				view.unrender();
				pm.detach();
				pm = null;
			}
		};

		@Override
		protected boolean postConstructPm() {
			if (!pm.init(host)) {
				// This happens if user clicked cancel during first render of
				// "Browse dialog"
				pm.detach();
				pm = null;
				return false;
			}
			return true;
		};
	};

	private DialogOpener<EncryptOnePm, EncryptOneView> encryptionWindowHost = new EncryptionWindowOpener(null);

	private class EncryptionWindowOpener extends DialogOpener<EncryptOnePm, EncryptOneView> {
		private String sourceFile;

		public EncryptionWindowOpener(String sourceFile) {
			super(EncryptOnePm.class, EncryptOneView.class, "action.encrypt");
			this.sourceFile = sourceFile;
		}

		EncryptOneHost host = new EncryptOneHost() {
			@Override
			public void handleClose() {
				view.unrender();
				pm.detach();
				pm = null;
			}

			@Override
			public Action getActionToOpenCertificatesList() {
				return keysListWindowHost.actionToOpenWindow;
			}
		};

		@Override
		protected boolean postConstructPm() {
			return pm.init(host, sourceFile);
		};
	};

	private DialogOpener<DecryptOnePm, DecryptOneView> decryptionWindowHost = new DecryptionWindowOpener(null);

	private class DecryptionWindowOpener extends DialogOpener<DecryptOnePm, DecryptOneView> {
		private String sourceFile;

		public DecryptionWindowOpener(String sourceFile) {
			super(DecryptOnePm.class, DecryptOneView.class, "action.decrypt");
			this.sourceFile = sourceFile;
		}

		DecryptOneHost host = new DecryptOneHost() {
			@Override
			public void handleClose() {
				view.unrender();
				pm.detach();
				pm = null;
			}

			@Override
			public Action getActionToOpenCertificatesList() {
				return keysListWindowHost.actionToOpenWindow;
			}
		};

		@Override
		protected boolean postConstructPm() {
			return pm.init(host, sourceFile);
		};
	};

	private DialogOpener<KeysListPm, KeysListView> keysListWindowHost = new DialogOpener<KeysListPm, KeysListView>(
			KeysListPm.class, KeysListView.class, "action.showKeysList") {
		@Override
		protected boolean postConstructPm() {
			pm.init(new KeysListHost() {
				@Override
				public void handleClose() {
					view.unrender();
					pm.detach();
					pm = null;
				}

				@Override
				public Action getActionImportKey() {
					return importKeyWindowHost.actionToOpenWindow;
				}
			});
			return true;
		};
	};

	private DialogOpener<AboutPm, AboutView> aboutWindowHost = new DialogOpener<AboutPm, AboutView>(AboutPm.class,
			AboutView.class, "term.aboutApp") {

		@Override
		protected boolean postConstructPm() {
			pm.init(new AboutHost() {
				@Override
				public void handleClose() {
					view.unrender();
					pm.detach();
					pm = null;
				}
			});
			return true;
		};
	};

	/**
	 * This is just a helper inner class to hold top windows hosts.
	 * 
	 * @author sergeyk
	 *
	 * @param <TPmType>
	 *            presentation model type
	 * @param <TViewType>
	 *            view type
	 */
	private abstract class DialogOpener<TPmType extends PresentationModelBase, TViewType extends ViewBase<TPmType>> {
		private Class<TPmType> pmClass;
		private Class<TViewType> viewClass;

		protected final Action actionToOpenWindow;
		protected TPmType pm;
		protected TViewType view;

		public DialogOpener(Class<TPmType> pmClass, Class<TViewType> viewClass, String openActionMessageCode) {
			this.pmClass = pmClass;
			this.viewClass = viewClass;

			actionToOpenWindow = new LocalizedAction(openActionMessageCode) {
				private static final long serialVersionUID = 2248174164525745404L;

				@Override
				public void actionPerformed(ActionEvent e) {
					openWindow();
				}
			};
		}

		protected void openWindow() {
			if (pm != null && view != null && view instanceof HasWindow) {
				log.debug("Window is already opened -- just bring it to top " + view);
				Window window = ((HasWindow) view).getWindow();
				window.setVisible(true);
				return;
			}

			pm = applicationContext.getBean(pmClass);
			if (!postConstructPm()) {
				pm = null;
				return;
			}

			if (view == null) {
				view = applicationContext.getBean(viewClass);
			}
			view.setPm(pm);
			view.renderTo(null);
		}

		/**
		 * NOTE: Override it if any further initialization of PM needed
		 * 
		 * @return true if we should proceed and open view. false if operation
		 *         should be canceled -- no view will be opened
		 */
		protected boolean postConstructPm() {
			return true;
		}
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
