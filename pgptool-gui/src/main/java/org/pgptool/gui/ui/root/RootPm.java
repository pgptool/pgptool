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
package org.pgptool.gui.ui.root;

import static org.pgptool.gui.app.Messages.text;

import com.google.common.eventbus.EventBus;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.app.Message;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.hintsforusage.hints.BuyMeCoffeeHint;
import org.pgptool.gui.tools.ClipboardUtil;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.ui.about.AboutHost;
import org.pgptool.gui.ui.about.AboutPm;
import org.pgptool.gui.ui.about.AboutView;
import org.pgptool.gui.ui.changekeypassword.ChangeKeyPasswordHost;
import org.pgptool.gui.ui.changekeypassword.ChangeKeyPasswordPm;
import org.pgptool.gui.ui.changekeypassword.ChangeKeyPasswordView;
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
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogPm.GetKeyPasswordPo;
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
import org.pgptool.gui.ui.tools.UrlOpener;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.dto.ApplicationExitUsage;
import org.pgptool.gui.usage.dto.ApplicationStartUsage;
import org.pgptool.gui.usage.dto.CommandLineArgsUsage;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import ru.skarpushin.swingpm.base.HasWindow;
import ru.skarpushin.swingpm.base.ViewBase;
import ru.skarpushin.swingpm.tools.edt.Edt;

/**
 * Root presentation model
 *
 * @author Sergey Karpushin
 */
public class RootPm implements InitializingBean, GlobalAppActions {
  private static final Logger log = Logger.getLogger(RootPm.class);

  public static RootPm INSTANCE;

  private final EntryPoint entryPoint;
  private final ApplicationContext applicationContext;
  private final EventBus eventBus;
  private final UpdatesPolicy updatesPolicy;

  private MainFramePm mainFramePm;
  private MainFrameView mainFrameView;
  private final HintsCoordinator hintsCoordinator;
  private final KeysExporterUi keysExporterUi;
  private final KeyFilesOperations keyFilesOperations;
  private final UsageLogger usageLogger;
  private final BuyMeCoffeeHint buyMeCoffeeHint;
  private TempFolderChooserPm tempFolderChooserPm;

  public RootPm(
      EntryPoint entryPoint,
      ApplicationContext applicationContext,
      EventBus eventBus,
      UpdatesPolicy updatesPolicy,
      HintsCoordinator hintsCoordinator,
      KeysExporterUi keysExporterUi,
      KeyFilesOperations keyFilesOperations,
      UsageLogger usageLogger,
      BuyMeCoffeeHint buyMeCoffeeHint) {
    this.entryPoint = entryPoint;
    this.applicationContext = applicationContext;
    this.eventBus = eventBus;
    this.updatesPolicy = updatesPolicy;
    this.hintsCoordinator = hintsCoordinator;
    this.keysExporterUi = keysExporterUi;
    this.keyFilesOperations = keyFilesOperations;
    this.usageLogger = usageLogger;
    this.buyMeCoffeeHint = buyMeCoffeeHint;

    INSTANCE = this;
  }

  @Override
  public void afterPropertiesSet() {
    eventBus.register(this);
  }

  public void present(String[] commandLineArgs) {
    try {
      usageLogger.write(new ApplicationStartUsage());
      updatesPolicy.start(checkForUpdatesDialog);
      openMainFrameWindow();
      processCommandLine(commandLineArgs);
      // THINK: Perhaps we'd better not open main frame if we were invoked
      // just in order to encrypt/decrypt something
    } catch (Throwable t) {
      try {
        log.error("Failed to start application", t);
        UiUtils.messageBox(
            /* by design */ null,
            ConsoleExceptionUtils.getAllMessages(t),
            Messages.get("exception.unexpected.failedToStartupApplication"),
            MessageSeverity.ERROR);
      } finally {
        // NOTE: We actually mean to exit application
        exitApplication(-1);
      }
    }
  }

  /**
   * @param commandLineArgs
   */
  public void processCommandLine(String[] commandLineArgs) {
    if (commandLineArgs == null || commandLineArgs.length == 0) {
      try {
        if (mainFrameView != null) {
          mainFrameView.bringToFront();
        }
      } catch (Throwable t) {
        EntryPoint.reportExceptionToUser(null, "exception.unexpected", t);
      }
      return;
    }

    try {
      usageLogger.write(new CommandLineArgsUsage(commandLineArgs));

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
          UiUtils.messageBox(
              /* by design */ null,
              "Program argument cannot be handled: " + file,
              text("term.attention"),
              MessageSeverity.WARNING);
        }
      }
    } catch (Throwable t) {
      EntryPoint.reportExceptionToUser(null, "error.failedToProcessCommandLine", t);
    }
  }

  private void openNewDecryptionWindow(final String sourceFile) {
    Edt.invokeOnEdtAndWait(
        new Runnable() {
          @Override
          public void run() {
            // NOTE: Assuming it is blocking operation!
            // Remember: View is singleton here
            new DecryptionWindowOpener(sourceFile)
                .actionToOpenWindow.actionPerformed(/* by design */ null);
          }
        });
  }

  private void openNewEncryptionWindow(final String sourceFile) {
    Edt.invokeOnEdtAndWait(
        new Runnable() {
          @Override
          public void run() {
            // NOTE: Assuming it is blocking operation!
            // Remember: View is singleton here
            new EncryptionWindowOpener(sourceFile)
                .actionToOpenWindow.actionPerformed(/* by design */ null);
          }
        });
  }

  private void exitApplication(int statusCode) {
    usageLogger.write(new ApplicationExitUsage());
    entryPoint.tearDownContext();
    System.exit(statusCode);
  }

  private final MainFrameHost mainFrameHost =
      new MainFrameHost() {
        private final Action openHelp =
            UrlOpener.buildAction("action.openHelp", "https://pgptool.github.io/#help", this);
        private final Action openFaq =
            UrlOpener.buildAction("action.openFaq", "https://pgptool.github.io/#faq", this);
        private final Action reportIssue =
            UrlOpener.buildAction(
                "action.reportIssue", "https://github.com/pgptool/pgptool/issues", this);

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
        public void openEncryptDialogFor(String decryptedFile, ActionEvent originEvent) {
          new EncryptionWindowOpener(decryptedFile).actionToOpenWindow.actionPerformed(originEvent);
        }

        @Override
        public void openDecryptDialogFor(String encryptedFile, ActionEvent originEvent) {
          new DecryptionWindowOpener(encryptedFile).actionToOpenWindow.actionPerformed(originEvent);
        }

        @Override
        public Action getActionCreateKey() {
          return createKeyWindowHost.actionToOpenWindow;
        }

        @Override
        public void openEncryptBackMultipleFor(
            Set<String> decryptedFiles, ActionEvent originEvent) {
          new EncryptBackManyWindowOpener(decryptedFiles)
              .actionToOpenWindow.actionPerformed(originEvent);
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

        @Override
        public Action getActionBuyMeCoffee() {
          return buyMeCoffeeHint.getBuyMeCoffeeAction();
        }

        @Override
        public Action getActionFaq() {
          return openFaq;
        }

        @Override
        public Action getActionHelp() {
          return openHelp;
        }

        @Override
        public Action getActionImportKeyFromText() {
          return importKeyFromClipboard;
        }

        @Override
        public Action getActionReportIssue() {
          return reportIssue;
        }
      };

  private void openMainFrameWindow() {
    mainFramePm = applicationContext.getBean(MainFramePm.class);
    mainFramePm.init(null, mainFrameHost, updatesPolicy);

    MainFrameView view = getMainFrameView();
    view.setPm(mainFramePm);
    view.renderTo(null);

    hintsCoordinator.setHintsHolder(mainFramePm);
  }

  protected Action actionShowTempFolderChooser =
      new LocalizedActionEx("term.changeTempFolderForDecrypted", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          tempFolderChooserPm = applicationContext.getBean(TempFolderChooserPm.class);
          tempFolderChooserPm.present(e);
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
      EntryPoint.reportExceptionToUser(null, "exception.unexpected.failedToCloseConfig", t);
    }
  }

  private MainFrameView getMainFrameView() {
    if (mainFrameView == null) {
      mainFrameView = applicationContext.getBean(MainFrameView.class);
    }
    return mainFrameView;
  }

  public Window findMainFrameWindow() {
    if (mainFrameView != null) {
      return mainFrameView.getWindow();
    }
    return null;
  }

  private class ImportKeyDialogOpener
      extends DialogOpener<KeyImporterHost, List<Key>, KeyImporterPm, KeyImporterView> {
    private List<Key> keys;

    public ImportKeyDialogOpener() {
      super(KeyImporterPm.class, KeyImporterView.class, "action.importKey");
    }

    public ImportKeyDialogOpener(List<Key> keys) {
      super(KeyImporterPm.class, KeyImporterView.class, "action.importKey");
      this.keys = keys;
    }

    protected KeyImporterHost host =
        new KeyImporterHost() {
          @Override
          public void handleImporterFinished() {
            view.unrender();
            pm.detach();
            pm = null;
          }
        };

    @Override
    protected KeyImporterHost getHost() {
      return host;
    }

    @Override
    protected List<Key> getInitParams() {
      return keys;
    }
  }

  private final ImportKeyDialogOpener importKeyWindowHost = new ImportKeyDialogOpener();

  protected EncryptTextDialogOpener encryptTextHost = new EncryptTextDialogOpener(new HashSet<>());

  class EncryptTextDialogOpener
      extends DialogOpener<EncryptTextHost, Set<String>, EncryptTextPm, EncryptTextView> {
    private final Set<String> preselectedKeyIds;

    public EncryptTextDialogOpener(Set<String> preselectedKeyIds) {
      super(EncryptTextPm.class, EncryptTextView.class, "action.encryptText");
      this.preselectedKeyIds = preselectedKeyIds;
    }

    EncryptTextHost host =
        new EncryptTextHost() {
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
    protected EncryptTextHost getHost() {
      return host;
    }

    @Override
    protected Set<String> getInitParams() {
      return preselectedKeyIds;
    }
    ;
  }
  ;

  private class GetKeyPasswordDialogOpener
      extends DialogOpener<
          GetKeyPasswordDialogHost,
          GetKeyPasswordPo,
          GetKeyPasswordDialogPm,
          GetKeyPasswordDialogView> {
    private final Set<String> keysIds;
    private final Message purpose;
    private final KeyAndPasswordCallback keyAndPasswordCallback;

    public GetKeyPasswordDialogOpener(
        Set<String> keysIds, Message purpose, KeyAndPasswordCallback keyAndPasswordCallback) {
      super(
          GetKeyPasswordDialogPm.class,
          GetKeyPasswordDialogView.class,
          "action.providePasswordForTheKey");
      this.keysIds = keysIds;
      this.purpose = purpose;
      this.keyAndPasswordCallback = keyAndPasswordCallback;
    }

    GetKeyPasswordDialogHost host =
        new GetKeyPasswordDialogHost() {
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
    protected GetKeyPasswordDialogHost getHost() {
      return host;
    }

    @Override
    protected void doRenderView(Window optionalOrigin) {
      view.renderTo(optionalOrigin);
    }

    @Override
    protected GetKeyPasswordPo getInitParams() {
      return new GetKeyPasswordPo(keysIds, purpose, keyAndPasswordCallback);
    }
  }
  ;

  private final DialogOpener<DecryptTextHost, Void, DecryptTextPm, DecryptTextView>
      decryptTextHost =
          new DialogOpener<>(DecryptTextPm.class, DecryptTextView.class, "action.decryptText") {

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
              public void askUserForKeyAndPassword(
                  Set<String> keysIds,
                  Message purpose,
                  KeyAndPasswordCallback keyAndPasswordCallback,
                  ActionEvent originEvent) {
                new GetKeyPasswordDialogOpener(keysIds, purpose, keyAndPasswordCallback)
                    .actionToOpenWindow.actionPerformed(originEvent);
              }

              @Override
              public void openEncryptText(Set<String> recipientsList, ActionEvent originEvent) {
                new EncryptTextDialogOpener(recipientsList)
                    .actionToOpenWindow.actionPerformed(originEvent);
              }
            }
            ;

            private final DecryptTextHostImpl host = new DecryptTextHostImpl();

            @Override
            protected DecryptTextHost getHost() {
              return host;
            }

            @Override
            protected Void getInitParams() {
              return null;
            }
            ;
          };

  private class EncryptBackManyWindowOpener
      extends DialogOpener<
          EncryptBackMultipleHost, Set<String>, EncryptBackMultiplePm, EncryptBackMultipleView> {
    private final Set<String> decryptedFiles;

    public EncryptBackManyWindowOpener(Set<String> decryptedFiles) {
      super(EncryptBackMultiplePm.class, EncryptBackMultipleView.class, "encrypBackMany.action");
      this.decryptedFiles = decryptedFiles;
    }

    EncryptBackMultipleHost host =
        new EncryptBackMultipleHost() {
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
    protected EncryptBackMultipleHost getHost() {
      return host;
    }

    @Override
    protected Set<String> getInitParams() {
      return decryptedFiles;
    }
    ;
  }
  ;

  private final EncryptionWindowOpener encryptionWindowHost = new EncryptionWindowOpener(null);

  private class EncryptionWindowOpener
      extends DialogOpener<EncryptOneHost, String, EncryptOnePm, EncryptOneView> {
    private final String sourceFile;

    public EncryptionWindowOpener(String sourceFile) {
      super(EncryptOnePm.class, EncryptOneView.class, "action.encrypt");
      this.sourceFile = sourceFile;
    }

    EncryptOneHost host =
        new EncryptOneHost() {
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
    protected EncryptOneHost getHost() {
      return host;
    }

    @Override
    protected String getInitParams() {
      return sourceFile;
    }
    ;
  }
  ;

  private final DecryptionWindowOpener decryptionWindowHost = new DecryptionWindowOpener(null);

  private class DecryptionWindowOpener
      extends DialogOpener<DecryptOneDialogHost, String, DecryptOneDialogPm, DecryptOneDialogView> {
    private final String sourceFile;

    public DecryptionWindowOpener(String sourceFile) {
      super(DecryptOneDialogPm.class, DecryptOneDialogView.class, "action.decrypt");
      this.sourceFile = sourceFile;
    }

    DecryptOneDialogHost host =
        new DecryptOneDialogHost() {
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
    protected DecryptOneDialogHost getHost() {
      return host;
    }

    @Override
    protected String getInitParams() {
      return sourceFile;
    }
    ;
  }
  ;

  private final DialogOpener<CreateKeyHost, Void, CreateKeyPm, CreateKeyView> createKeyWindowHost =
      new DialogOpener<>(CreateKeyPm.class, CreateKeyView.class, "action.createPgpKey") {
        @Override
        protected CreateKeyHost getHost() {
          return new CreateKeyHost() {
            @Override
            public void handleClose() {
              view.unrender();
              pm.detach();
              pm = null;
            }
          };
        }

        @Override
        protected Void getInitParams() {
          return null;
        }
        ;
      };

  private class ChangeKeyPasswordDialogOpener
      extends DialogOpener<ChangeKeyPasswordHost, Key, ChangeKeyPasswordPm, ChangeKeyPasswordView> {
    private final Key key;

    public ChangeKeyPasswordDialogOpener(Key key) {
      super(ChangeKeyPasswordPm.class, ChangeKeyPasswordView.class, "action.changePassphrase");
      this.key = key;
    }

    @Override
    protected ChangeKeyPasswordHost getHost() {
      return new ChangeKeyPasswordHost() {
        @Override
        public void handleClose() {
          view.unrender();
          pm.detach();
          pm = null;
        }
      };
    }

    @Override
    protected Key getInitParams() {
      return key;
    }
    ;
  }

  private final DialogOpener<KeysListHost, Void, KeysListPm, KeysListView> keysListWindowHost =
      new DialogOpener<>(KeysListPm.class, KeysListView.class, "action.showKeysList") {

        @Override
        protected KeysListHost getHost() {
          return new KeysListHost() {
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

            @Override
            public void changeKeyPassphrase(Key key, ActionEvent originalEvent) {
              new ChangeKeyPasswordDialogOpener(key)
                  .actionToOpenWindow.actionPerformed(originalEvent);
            }
          };
        }

        @Override
        protected Void getInitParams() {
          return null;
        }
        ;
      };

  private final Action importKeyFromClipboard =
      new LocalizedActionEx("action.importKeyFromText", this) {
        private static final long serialVersionUID = -3347918300952342578L;

        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          String clipboard = ClipboardUtil.tryGetClipboardText();
          if (!StringUtils.hasText(clipboard)) {
            UiUtils.messageBox(
                e,
                text("warning.noTextInClipboard"),
                text("term.attention"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
          }

          List<Key> keys;
          try {
            keys = keyFilesOperations.readKeysFromText(clipboard);
          } catch (Throwable t) {
            GenericException t2 =
                new GenericException("warning.failedToImportPgpKeyFromClipboard", t);
            log.warn(t2.getMessage(), t2);
            UiUtils.messageBox(
                e,
                ConsoleExceptionUtils.getAllMessages(t2),
                text("term.attention"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
          }

          // Open list of processed keys
          new ImportKeyDialogOpener(new ArrayList<>(keys)).actionToOpenWindow.actionPerformed(e);
        }
      };

  public class CheckForUpdatesDialog
      extends DialogOpener<
          CheckForUpdatesHost, UpdatesPolicy, CheckForUpdatesPm, CheckForUpdatesView> {
    public CheckForUpdatesDialog() {
      super(CheckForUpdatesPm.class, CheckForUpdatesView.class, "action.checkForUpdates");
    }

    public CheckForUpdatesHost host =
        new CheckForUpdatesHost() {
          @Override
          public void handleClose() {
            view.unrender();
          }
        };

    @Override
    protected CheckForUpdatesHost getHost() {
      return host;
    }

    @Override
    protected UpdatesPolicy getInitParams() {
      return null;
    }
    ;
  }

  private final CheckForUpdatesDialog checkForUpdatesDialog = new CheckForUpdatesDialog();

  private final DialogOpener<AboutHost, Void, AboutPm, AboutView> aboutWindowHost =
      new DialogOpener<>(AboutPm.class, AboutView.class, "term.aboutApp") {
        @Override
        protected AboutHost getHost() {
          return new AboutHost() {
            @Override
            public void handleClose() {
              view.unrender();
              pm.detach();
              pm = null;
            }
          };
        }

        @Override
        protected Void getInitParams() {
          return null;
        }
        ;
      };

  /**
   * This is just a helper inner class to hold top windows hosts.
   *
   * @author Sergey Karpushin
   * @param <TPmType> presentation model type
   * @param <TViewType> view type
   */
  private abstract class DialogOpener<
      PMHT,
      PMPO,
      TPmType extends PresentationModelBaseEx<PMHT, PMPO>,
      TViewType extends ViewBase<TPmType>> {
    private final Class<TPmType> pmClass;
    private final Class<TViewType> viewClass;

    public final Action actionToOpenWindow;
    protected TPmType pm;
    protected TViewType view;

    public DialogOpener(
        Class<TPmType> pmClass, Class<TViewType> viewClass, String openActionMessageCode) {
      this.pmClass = pmClass;
      this.viewClass = viewClass;

      actionToOpenWindow =
          new LocalizedActionEx(openActionMessageCode, this) {
            private static final long serialVersionUID = 2248174164525745404L;

            @Override
            public void actionPerformed(ActionEvent e) {
              super.actionPerformed(e);
              try {
                openWindow(e);
              } catch (Throwable t) {
                Object[] messageArgs = {pmClass.getSimpleName()};
                EntryPoint.reportExceptionToUser(e, "failed.toOpenWindow", t, messageArgs);
              }
            }
          };
    }

    protected void openWindow(ActionEvent originAction) {
      Window optionalOrigin = UiUtils.findWindow(originAction);

      if (pm != null && view != null && view instanceof HasWindow) {
        log.debug("Window is already opened -- just bring it to top " + view);
        Window window = ((HasWindow) view).getWindow();
        // TBD: Move this code to same place where we do center window
        UiUtils.centerWindow(window, optionalOrigin);
        window.setVisible(true);
        return;
      }

      if (pm == null) {
        pm = applicationContext.getBean(pmClass);
        if (!initPm(originAction)) {
          if (pm != null) {
            pm.detach();
            pm = null;
          }
          return;
        }
      }

      if (view == null) {
        buildViewInstance();
      }
      view.setPm(pm);
      doRenderView(optionalOrigin);
    }

    protected void doRenderView(Window optionalOrigin) {
      view.renderTo(optionalOrigin);
    }

    protected void buildViewInstance() {
      view = applicationContext.getBean(viewClass);
    }

    /**
     * NOTE: Override it if any further initialization of PM needed
     *
     * @return true if we should proceed and open view. false if operation should be canceled -- no
     *     view will be opened
     */
    protected boolean initPm(ActionEvent originAction) {
      return pm.init(originAction, getHost(), getInitParams());
    }

    protected abstract PMHT getHost();

    protected abstract PMPO getInitParams();
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
  public void triggerPrivateKeyExport(Key key, ActionEvent originEvent) {
    keysExporterUi.exportPrivateKey((Key) key, originEvent);
  }
}
