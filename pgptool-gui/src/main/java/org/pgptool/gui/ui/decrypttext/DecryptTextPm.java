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
package org.pgptool.gui.ui.decrypttext;

import static org.pgptool.gui.app.Messages.text;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Message;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.MatchedKey;
import org.pgptool.gui.encryption.implpgp.SymmetricEncryptionIsNotSupportedException;
import org.pgptool.gui.tools.ClipboardUtil;
import org.pgptool.gui.ui.decryptonedialog.KeyAndPasswordCallback;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.dto.DecryptTextRecipientsIdentifiedUsage;
import org.pgptool.gui.usage.dto.DecryptedTextUsage;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class DecryptTextPm extends PresentationModelBaseEx<DecryptTextHost, Void> {
  private static final Logger log = Logger.getLogger(DecryptTextPm.class);

  private final KeyRingService keyRingService;
  private final EncryptionService encryptionService;
  private final UsageLogger usageLogger;

  private Set<String> keysIds;
  private PasswordDeterminedForKey keyAndPassword;

  private ModelProperty<String> sourceText;
  private ModelProperty<String> recipients;
  protected Set<String> recipientsList;
  private ModelProperty<String> targetText;
  private ModelProperty<Boolean> isShowMissingPrivateKeyWarning;

  public DecryptTextPm(
      KeyRingService keyRingService, EncryptionService encryptionService, UsageLogger usageLogger) {
    this.keyRingService = keyRingService;
    this.encryptionService = encryptionService;
    this.usageLogger = usageLogger;
  }

  @Override
  public boolean init(ActionEvent originAction, DecryptTextHost host, Void initParams) {
    super.init(originAction, host, initParams);
    Preconditions.checkArgument(host != null);

    if (!ensureWeHaveAtLeastOnePrivateKey()) {
      return false;
    }

    initModelProperties();

    String clipboard = ClipboardUtil.tryGetClipboardText();
    Set<String> keysForDecryption = findKeysForTextDecryption(clipboard, true);
    if (!keysForDecryption.isEmpty()) {
      sourceText.setValueByOwner(clipboard);
    }

    return true;
  }

  private void initModelProperties() {
    sourceText = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "textToEncrypt");
    sourceText.getModelPropertyAccessor().addPropertyChangeListener(onSourceTextChanged);
    actionDecrypt.setEnabled(false);

    targetText = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "encryptedText");
    targetText.getModelPropertyAccessor().addPropertyChangeListener(onTargetTextChanged);
    actionCopyTargetToClipboard.setEnabled(false);

    recipients = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "recipients");
    actionReply.setEnabled(false);

    isShowMissingPrivateKeyWarning =
        new ModelProperty<>(
            this, new ValueAdapterHolderImpl<>(false), "isShowMissingPrivateKeyWarning");
  }

  private final PropertyChangeListener onSourceTextChanged =
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          boolean hasText = StringUtils.hasText((String) evt.getNewValue());
          if (hasText) {
            // Discover possible keys
            keysIds = findKeysForTextDecryption(sourceText.getValue(), true);
            // Show list of emails
            recipientsList = new HashSet<>(keysIds);
            recipients.setValueByOwner(collectRecipientsNames(keysIds));
            // See if we have at least one key suitable for decryption
            isShowMissingPrivateKeyWarning.setValueByOwner(
                shouldWeShowMissingPrivateKeyWarning(keysIds));
            actionDecrypt.setEnabled(
                !keysIds.isEmpty() && !isShowMissingPrivateKeyWarning.getValue());
          } else {
            actionDecrypt.setEnabled(false);
            keysIds = Collections.emptySet();
            recipientsList = new HashSet<>();
            recipients.setValueByOwner("");
            isShowMissingPrivateKeyWarning.setValueByOwner(false);
          }
        }
      };

  private final PropertyChangeListener onTargetTextChanged =
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          boolean hasText = StringUtils.hasText((String) evt.getNewValue());
          actionCopyTargetToClipboard.setEnabled(hasText);
        }
      };

  private boolean ensureWeHaveAtLeastOnePrivateKey() {
    if (doWeHaveAtLeastOnePrivateKey()) {
      return true;
    }
    UiUtils.messageBox(
        originAction,
        text("phrase.noKeysForDecryption"),
        text("term.attention"),
        MessageSeverity.WARNING);
    host.getActionToOpenCertificatesList().actionPerformed(originAction);
    if (!doWeHaveAtLeastOnePrivateKey()) {
      return false;
    }
    return true;
  }

  private boolean doWeHaveAtLeastOnePrivateKey() {
    List<Key> keys = keyRingService.readKeys();
    if (keys.isEmpty()) {
      return false;
    }
    return keys.stream().anyMatch(x -> x.getKeyData().isCanBeUsedForDecryption());
  }

  private boolean shouldWeShowMissingPrivateKeyWarning(Set<String> options) {
    if (options.isEmpty()) {
      return false;
    }
    List<MatchedKey> matchingKeys = keyRingService.findMatchingDecryptionKeys(options);
    return CollectionUtils.isEmpty(matchingKeys);
  }

  public ModelPropertyAccessor<String> getSourceText() {
    return sourceText.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getTargetText() {
    return targetText.getModelPropertyAccessor();
  }

  private void decrypt(ActionEvent originEvent)
      throws UnsupportedEncodingException, SymmetricEncryptionIsNotSupportedException {
    findKeysForTextDecryption(sourceText.getValue(), false);
    Preconditions.checkState(
        !CollectionUtils.isEmpty(keysIds),
        "Invalid state. Expected to have list of keys which could be used for decryption");
    usageLogger.write(new DecryptTextRecipientsIdentifiedUsage(keysIds));

    // Request password for decryption key
    if (keyAndPassword != null && keysIds.contains(keyAndPassword.getDecryptionKeyId())) {
      keyAndPasswordCallback.onKeyPasswordResult(keyAndPassword);
    } else {
      Message purpose = new Message("phrase.needKeyToDecryptFromClipboard");
      host.askUserForKeyAndPassword(keysIds, purpose, keyAndPasswordCallback, originEvent);
    }
  }

  private Set<String> findKeysForTextDecryption(String textToDecrypt, boolean failSilently) {
    try {
      ByteArrayInputStream inputStream = new ByteArrayInputStream(textToDecrypt.getBytes("UTF-8"));
      return encryptionService.findKeyIdsForDecryption(inputStream);
    } catch (Throwable t) {
      if (failSilently) {
        log.warn(
            "Text in clipboard is not an encrypted content or there are no keys suitable for decryption");
        return Collections.emptySet();
      } else {
        throw new RuntimeException("Failed to determine keys for text decryption", t);
      }
    }
  }

  private String pasteFromClipboard(ActionEvent originAction) {
    String clipboard = ClipboardUtil.tryGetClipboardText();
    if (!StringUtils.hasText(clipboard)) {
      UiUtils.messageBox(
          originAction,
          text("warning.noTextInClipboard"),
          text("term.attention"),
          JOptionPane.INFORMATION_MESSAGE);
      return null;
    }
    sourceText.setValueByOwner(clipboard);
    return clipboard;
  }

  private String collectRecipientsNames(Set<String> keysIds) {
    StringBuilder ret = new StringBuilder();
    for (String keyId : keysIds) {
      if (!ret.isEmpty()) {
        ret.append(", ");
      }
      Key key = keyRingService.findKeyById(keyId);
      if (key == null) {
        ret.append(keyId);
      } else {
        ret.append(key.getKeyInfo().getUser());
      }
    }
    return ret.toString();
  }

  public ModelPropertyAccessor<String> getRecipients() {
    return recipients.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<Boolean> getIsShowMissingPrivateKeyWarning() {
    return isShowMissingPrivateKeyWarning.getModelPropertyAccessor();
  }

  private final KeyAndPasswordCallback keyAndPasswordCallback =
      new KeyAndPasswordCallback() {
        @Override
        public void onKeyPasswordResult(PasswordDeterminedForKey keyAndPassword) {
          ActionEvent surrogateEvent =
              UiUtils.actionEvent(findRegisteredWindowIfAny(), "onKeyPasswordResult");
          try {
            DecryptTextPm.this.keyAndPassword = keyAndPassword;
            if (keyAndPassword == null) {
              // this means user might have just canceled the dialog
              if (isShowMissingPrivateKeyWarning.getValue()) {
                UiUtils.messageBox(
                    surrogateEvent,
                    text("error.noMatchingKeysRegistered"),
                    text("term.error"),
                    MessageSeverity.ERROR);
              }
              return;
            }

            // Decrypt
            String decryptedText =
                encryptionService.decryptText(sourceText.getValue(), keyAndPassword);
            usageLogger.write(new DecryptedTextUsage(keyAndPassword.getDecryptionKeyId()));

            // Set target text
            targetText.setValueByOwner(decryptedText);
            actionReply.setEnabled(true);
          } catch (Throwable t) {
            EntryPoint.reportExceptionToUser(surrogateEvent, "error.cantParseEncryptedText", t);
          }
        }
      };

  protected final Action actionReply =
      new LocalizedActionEx("action.reply", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          host.handleClose();
          host.openEncryptText(recipientsList, e);
        }
      };

  protected final Action actionCopyTargetToClipboard =
      new LocalizedActionEx("action.copyToClipboard", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          ClipboardUtil.setClipboardText(targetText.getValue());
        }
      };

  protected final Action actionPasteAndDecrypt =
      new LocalizedActionEx("action.decryptFromClipboard", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          try {
            String clipboard = pasteFromClipboard(e);
            if (clipboard == null) {
              return;
            }
            decrypt(e);
          } catch (Throwable t) {
            EntryPoint.reportExceptionToUser(e, "error.cantParseEncryptedText", t);
          }
        }
      };

  protected final Action actionDecrypt =
      new LocalizedActionEx("action.decrypt", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          try {
            Preconditions.checkState(
                StringUtils.hasText(sourceText.getValue()),
                "Action must not be invoked if source text is empty");
            decrypt(e);
          } catch (Throwable t) {
            EntryPoint.reportExceptionToUser(e, "error.cantParseEncryptedText", t);
          }
        }
      };

  protected final Action actionCancel =
      new LocalizedActionEx("action.close", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          host.handleClose();
        }
      };
}
