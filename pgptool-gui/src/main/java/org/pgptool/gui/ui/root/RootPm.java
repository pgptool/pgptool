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
package org.pgptool.gui.ui.root;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Message;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.tools.ClipboardUtil;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.ui.about.AboutHost;
import org.pgptool.gui.ui.about.AboutPm;
import org.pgptool.gui.ui.about.AboutView;
import org.pgptool.gui.ui.checkForUpdates.CheckForUpdatesHost;
import org.pgptool.gui.ui.checkForUpdates.CheckForUpdatesPm;
import org.pgptool.gui.ui.checkForUpdates.CheckForUpdatesView;
import org.pgptool.gui.ui.checkForUpdates.UpdatesPolicy;
import org.pgptool.gui.ui.createkey.CreateKeyHost;
import org.pgptool.gui.ui.createkey.CreateKeyPm;
import org.pgptool.gui.ui.createkey.CreateKeyView;
import org.pgptool.gui.ui.decryptone.DecryptOnePm;
import org.pgptool.gui.ui.decryptonedialog.DecryptOneDialogHost;
import org.pgptool.gui.ui.decryptonedialog.DecryptOneDialogPm;
import org.pgptool.gui.ui.decryptonedialog.DecryptOneDialogView;
import org.pgptool.gui.ui.decryptonedialog.KeyAndPasswordCallback;
import org.pgptool.gui.ui.decrypttext.DecryptTextHost;
import org.pgptool.gui.ui.decrypttext.DecryptTextPm;
import org.pgptool.gui.ui.decrypttext.DecryptTextView;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptBackMultipleHost;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptBackMultiplePm;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptBackMultipleView;
import org.pgptool.gui.ui.encryptone.EncryptOneHost;
import org.pgptool.gui.ui.encryptone.EncryptOnePm;
import org.pgptool.gui.ui.encryptone.EncryptOneView;
import org.pgptool.gui.ui.encrypttext.EncryptTextHost;
import org.pgptool.gui.ui.encrypttext.EncryptTextPm;
import org.pgptool.gui.ui.encrypttext.EncryptTextView;
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogHost;
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogPm;
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogView;
import org.pgptool.gui.ui.importkey.KeyImporterHost;
import org.pgptool.gui.ui.importkey.KeyImporterPm;
import org.pgptool.gui.ui.importkey.KeyImporterView;
import org.pgptool.gui.ui.keyslist.KeysExporterUi;
import org.pgptool.gui.ui.keyslist.KeysListHost;
import org.pgptool.gui.ui.keyslist.KeysListPm;
import org.pgptool.gui.ui.keyslist.KeysListView;
import org.pgptool.gui.ui.mainframe.MainFrameHost;
import org.pgptool.gui.ui.mainframe.MainFramePm;
import org.pgptool.gui.ui.mainframe.MainFrameView;
import org.pgptool.gui.ui.tempfolderfordecrypted.TempFolderChooserPm;
import org.pgptool.gui.ui.tools.UiUtils;
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
 * @author Sergey Karpushin
 * 
 */
public class RootPm implements ApplicationContextAware, InitializingBean, GlobalAppActions {
	private static Logger log = Logger.getLogger(RootPm.class);

	@Autowired
	private EntryPoint entryPoint;
	private ApplicationContext applicationContext;
	private EventBus eventBus;
	private UpdatesPolicy updatesPolicy;

	private MainFramePm mainFramePm;
	private MainFrameView mainFrameView;
	@Autowired
	private HintsCoordinator hintsCoordinator;
	@Autowired
	private KeysExporterUi keysExporterUi;
	@Autowired
	private KeyFilesOperations keyFilesOperations;

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
			updatesPolicy.start(checkForUpdatesDialog);
			openMainFrameWindow();
			processCommandLine(commandLineArgs);
			// THINK: Perhaps we'd better not open main frame if we were invoked
			// just in order to encrypt/decrypt something
		} catch (Throwable t) {
			try {
				log.error("Failed to start application", t);
				UiUtils.messageBox(ConsoleExceptionUtils.getAllMessages(t),
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
	 */
	public void processCommandLine(String[] commandLineArgs) {
		try {
			if (commandLineArgs == null || commandLineArgs.length == 0) {
				if (mainFrameView != null) {
					mainFrameView.bringToFront();
				}
				return;
			}

			if (commandLineArgs.length > 1) {
				log.warn("As of now application is not designed to handle more than 1 input file.");
			}

			// NOTE: We'll need to learn how to handle more than 1 input file
			// AND MAKE SURE UX will not suffer.
			// https://github.com/skarpushin/pgptool/issues/9
			for (int i = 0; i < 1 /* commandLineArgs.length */; i++) {
				String file = commandLineArgs[i];
				if (DecryptOnePm.isItLooksLikeYourSourceFile(file)) {
					openNewDecryptionWindow(file);
				} else if (EncryptOnePm.isItLooksLikeYourSourceFile(file)) {
					openNewEncryptionWindow(file);
				} else {
					UiUtils.messageBox("Program argument cannot be handled: " + file, text("term.attention"),
							MessageSeverity.WARNING);
				}
			}
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser("error.failedToProcessCommandLine", t);
		}
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

		@Override
		public void openDecryptDialogFor(String encryptedFile) {
			new DecryptionWindowOpener(encryptedFile).actionToOpenWindow.actionPerformed(null);
		}

		@Override
		public Action getActionCreateKey() {
			return createKeyWindowHost.actionToOpenWindow;
		}

		@Override
		public void openEncryptBackMultipleFor(Set<String> decryptedFiles) {
			new EncryptBackManyWindowOpener(decryptedFiles).actionToOpenWindow.actionPerformed(null);
		}

		@Override
		public Action getActionForEncryptText() {
			return encryptTextHost.actionToOpenWindow;
		}

		@Override
		public Action getActionForDecryptText() {
			return decryptTextHost.actionToOpenWindow;
		}

		@Override
		public Action getActionCheckForUpdates() {
			return checkForUpdatesDialog.actionToOpenWindow;
		}

		private Action buyMeCoffee = buildAction("action.buyMeCoffee", "https://www.buymeacoffee.com/skarpushind");

		@SuppressWarnings("serial")
		private LocalizedAction buildAction(String messageCode, String url) {
			return new LocalizedAction(messageCode) {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						Desktop.getDesktop().browse(new URI(url));
					} catch (Throwable t) {
						EntryPoint.reportExceptionToUser("failed.toOpenBrowser", t);
					}
				}
			};
		}

		@Override
		public Action getActionBuyMeCoffee() {
			return buyMeCoffee;
		}

		private Action openFaq = buildAction("action.openFaq", "https://pgptool.github.io/#faq");

		@Override
		public Action getActionFaq() {
			return openFaq;
		}

		private Action openHelp = buildAction("action.openHelp", "https://pgptool.github.io/#help");

		@Override
		public Action getActionHelp() {
			return openHelp;
		}

		@Override
		public Action getActionImportKeyFromText() {
			return importKeyFromClipboard;
		}
	};

	private void openMainFrameWindow() {
		mainFramePm = applicationContext.getBean(MainFramePm.class);
		mainFramePm.init(mainFrameHost, updatesPolicy);
		MainFrameView view = getMainFrameView();
		view.setPm(mainFramePm);
		view.renderTo(null);

		hintsCoordinator.setHintsHolder(mainFramePm);
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

	private class ImportKeyDialogOpener extends DialogOpener<KeyImporterPm, KeyImporterView> {
		private List<Key> keys;

		public ImportKeyDialogOpener() {
			super(KeyImporterPm.class, KeyImporterView.class, "action.importKey");
		}

		public ImportKeyDialogOpener(List<Key> keys) {
			super(KeyImporterPm.class, KeyImporterView.class, "action.importKey");
			this.keys = keys;
		}

		protected KeyImporterHost host = new KeyImporterHost() {
			@Override
			public void handleImporterFinished() {
				view.unrender();
				pm.detach();
				pm = null;
			}
		};

		@Override
		protected boolean postConstructPm() {
			if (keys != null) {
				pm.init(host, keys);
				return true;
			}
			if (!pm.init(host)) {
				pm.detach();
				pm = null;
				return false;
			}
			return true;
		};
	}

	private DialogOpener<KeyImporterPm, KeyImporterView> importKeyWindowHost = new ImportKeyDialogOpener();

	protected EncryptTextDialogOpener encryptTextHost = new EncryptTextDialogOpener(new HashSet<>());

	class EncryptTextDialogOpener extends DialogOpener<EncryptTextPm, EncryptTextView> {
		private Set<String> preselectedKeyIds;

		public EncryptTextDialogOpener(Set<String> preselectedKeyIds) {
			super(EncryptTextPm.class, EncryptTextView.class, "action.encryptText");
			this.preselectedKeyIds = preselectedKeyIds;
		}

		EncryptTextHost host = new EncryptTextHost() {
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
			if (!pm.init(host, preselectedKeyIds)) {
				// This happens if user clicked cancel during first render of
				// "Browse dialog"
				pm.detach();
				pm = null;
				return false;
			}
			return true;
		};
	};

	private class GetKeyPasswordDialogOpener extends DialogOpener<GetKeyPasswordDialogPm, GetKeyPasswordDialogView> {
		private Set<String> keysIds;
		private Message purpose;
		private KeyAndPasswordCallback keyAndPasswordCallback;
		private Window parentWindow;

		public GetKeyPasswordDialogOpener(Set<String> keysIds, Message purpose,
				KeyAndPasswordCallback keyAndPasswordCallback, Window parentWindow) {
			super(GetKeyPasswordDialogPm.class, GetKeyPasswordDialogView.class, "action.providePasswordForTheKey");
			this.keysIds = keysIds;
			this.purpose = purpose;
			this.keyAndPasswordCallback = keyAndPasswordCallback;
			this.parentWindow = parentWindow;
		}

		GetKeyPasswordDialogHost host = new GetKeyPasswordDialogHost() {
			@Override
			public void handleClose() {
				if (view != null) {
					view.unrender();
				}
				pm.detach();
				pm = null;
			}
		};

		@Override
		protected boolean postConstructPm() {
			return pm.init(host, keysIds, purpose, keyAndPasswordCallback);
		};

		@Override
		protected void doRenderView() {
			view.renderTo(parentWindow);
		}
	};

	private DialogOpener<DecryptTextPm, DecryptTextView> decryptTextHost = new DialogOpener<DecryptTextPm, DecryptTextView>(
			DecryptTextPm.class, DecryptTextView.class, "action.decryptText") {

		class DecryptTextHostImpl implements DecryptTextHost {
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

			@Override
			public void askUserForKeyAndPassword(Set<String> keysIds, Message purpose,
					KeyAndPasswordCallback keyAndPasswordCallback, Window parentWindow) {
				new GetKeyPasswordDialogOpener(keysIds, purpose, keyAndPasswordCallback,
						parentWindow).actionToOpenWindow.actionPerformed(null);
			}

			@Override
			public void openEncryptText(Set<String> recipientsList) {
				new EncryptTextDialogOpener(recipientsList).actionToOpenWindow.actionPerformed(null);
			}
		};

		private DecryptTextHostImpl host = new DecryptTextHostImpl();

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

	private class EncryptBackManyWindowOpener extends DialogOpener<EncryptBackMultiplePm, EncryptBackMultipleView> {
		private Set<String> decryptedFiles;

		public EncryptBackManyWindowOpener(Set<String> decryptedFiles) {
			super(EncryptBackMultiplePm.class, EncryptBackMultipleView.class, "encrypBackMany.action");
			this.decryptedFiles = decryptedFiles;
		}

		EncryptBackMultipleHost host = new EncryptBackMultipleHost() {
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
			return pm.init(host, decryptedFiles);
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

	private DialogOpener<DecryptOneDialogPm, DecryptOneDialogView> decryptionWindowHost = new DecryptionWindowOpener(
			null);

	private class DecryptionWindowOpener extends DialogOpener<DecryptOneDialogPm, DecryptOneDialogView> {
		private String sourceFile;

		public DecryptionWindowOpener(String sourceFile) {
			super(DecryptOneDialogPm.class, DecryptOneDialogView.class, "action.decrypt");
			this.sourceFile = sourceFile;
		}

		DecryptOneDialogHost host = new DecryptOneDialogHost() {
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

	private DialogOpener<CreateKeyPm, CreateKeyView> createKeyWindowHost = new DialogOpener<CreateKeyPm, CreateKeyView>(
			CreateKeyPm.class, CreateKeyView.class, "action.createPgpKey") {
		@Override
		protected boolean postConstructPm() {
			pm.init(new CreateKeyHost() {
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

				@Override
				public Action getActionCreateKey() {
					return createKeyWindowHost.actionToOpenWindow;
				}

				@Override
				public Action getActionImportKeyFromText() {
					return importKeyFromClipboard;
				}
			});
			return true;
		};
	};

	private Action importKeyFromClipboard = new LocalizedAction("action.importKeyFromText") {
		private static final long serialVersionUID = -3347918300952342578L;

		@Override
		public void actionPerformed(ActionEvent e) {
			String clipboard = ClipboardUtil.tryGetClipboardText();
			if (clipboard == null) {
				UiUtils.messageBox(mainFrameView.getWindow(), text("warning.noTextInClipboard"), text("term.attention"),
						JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			Key key;
			try {
				key = keyFilesOperations.readKeyFromText(clipboard);
			} catch (Throwable t) {
				UiUtils.messageBox(mainFrameView.getWindow(), text("warning.textDoesNotRepresentAKey"),
						text("term.attention"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			// Open list of processed keys
			new ImportKeyDialogOpener(new ArrayList<>(Arrays.asList(key))).actionToOpenWindow.actionPerformed(e);
		}
	};

	public class CheckForUpdatesDialog extends DialogOpener<CheckForUpdatesPm, CheckForUpdatesView> {
		public CheckForUpdatesDialog() {
			super(CheckForUpdatesPm.class, CheckForUpdatesView.class, "action.checkForUpdates");
		}

		public CheckForUpdatesHost host = new CheckForUpdatesHost() {
			@Override
			public void handleClose() {
				view.unrender();
			}
		};

		@Override
		protected boolean postConstructPm() {
			pm.init(host, updatesPolicy);
			return true;
		};
	}

	private CheckForUpdatesDialog checkForUpdatesDialog = new CheckForUpdatesDialog();

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
	 * @author Sergey Karpushin
	 *
	 * @param <TPmType>
	 *            presentation model type
	 * @param <TViewType>
	 *            view type
	 */
	private abstract class DialogOpener<TPmType extends PresentationModelBase, TViewType extends ViewBase<TPmType>> {
		private Class<TPmType> pmClass;
		private Class<TViewType> viewClass;

		public final Action actionToOpenWindow;
		protected TPmType pm;
		protected TViewType view;

		public DialogOpener(Class<TPmType> pmClass, Class<TViewType> viewClass, String openActionMessageCode) {
			this.pmClass = pmClass;
			this.viewClass = viewClass;

			actionToOpenWindow = new LocalizedAction(openActionMessageCode) {
				private static final long serialVersionUID = 2248174164525745404L;

				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						openWindow();
					} catch (Throwable t) {
						log.error("Failed to open dialog " + pmClass.getSimpleName(), t);
						UiUtils.reportExceptionToUser("failed.toOpenWindow", t, pmClass.getSimpleName());
					}
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

			if (pm == null) {
				pm = applicationContext.getBean(pmClass);
				if (!postConstructPm()) {
					pm = null;
					return;
				}
			}

			if (view == null) {
				buildViewInstance();
			}
			view.setPm(pm);
			doRenderView();
		}

		protected void doRenderView() {
			view.renderTo(null);
		}

		protected void buildViewInstance() {
			view = applicationContext.getBean(viewClass);
		}

		/**
		 * NOTE: Override it if any further initialization of PM needed
		 * 
		 * @return true if we should proceed and open view. false if operation should be
		 *         canceled -- no view will be opened
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

	public UpdatesPolicy getUpdatesPolicy() {
		return updatesPolicy;
	}

	@Autowired
	public void setUpdatesPolicy(UpdatesPolicy updatesPolicy) {
		this.updatesPolicy = updatesPolicy;
	}

	@Override
	public Action getActionImportKey() {
		return importKeyWindowHost.actionToOpenWindow;
	}

	@Override
	public Action getActionCreateKey() {
		return createKeyWindowHost.actionToOpenWindow;
	}

	@Override
	public void triggerPrivateKeyExport(Key key) {
		keysExporterUi.exportPrivateKey((Key) key, mainFrameView.getWindow());
	}
}
