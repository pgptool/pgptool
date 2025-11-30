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
package org.pgptool.gui.ui.encrypttext;

import static org.pgptool.gui.app.Messages.text;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.tools.ClipboardUtil;
import org.pgptool.gui.ui.keyslist.ComparatorKeyByNameImpl;
import org.pgptool.gui.ui.tools.ListChangeListenerAnyEventImpl;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.dto.EncryptTextRecipientsUsage;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.summerb.validation.ValidationContext;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.lists.ModelListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultiSelInListProperty;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class EncryptTextPm extends PresentationModelBaseEx<EncryptTextHost, Set<String>> {
  private final KeyRingService keyRingService;
  private final EncryptionService encryptionService;
  private final UsageLogger usageLogger;

  private ModelMultiSelInListProperty<Key> selectedRecipients;
  private ModelListProperty<Key> availabileRecipients;

  private ModelProperty<String> sourceText;
  private ModelProperty<String> targetText;

  public EncryptTextPm(
      KeyRingService keyRingService, EncryptionService encryptionService, UsageLogger usageLogger) {
    this.keyRingService = keyRingService;
    this.encryptionService = encryptionService;
    this.usageLogger = usageLogger;
  }

  @Override
  public boolean init(
      ActionEvent originAction, EncryptTextHost host, Set<String> preselectedKeyIds) {
    super.init(originAction, host, preselectedKeyIds);
    Preconditions.checkArgument(host != null);

    if (!doWeHaveKeysToEncryptWith()) {
      return false;
    }

    initModelProperties();

    if (preselectedKeyIds != null) {
      Set<String> missedKeys = preselectRecipients(preselectedKeyIds);
      notifyUserOfMissingKeysIfAny(missedKeys);
    }

    return true;
  }

  private Set<String> preselectRecipients(Set<String> recipientsKeysIds) {
    selectedRecipients.getList().clear();
    Set<String> missedKeys = new HashSet<>();
    for (String keyId : recipientsKeysIds) {
      Optional<Key> key =
          availabileRecipients.getList().stream()
              .filter(x -> x.getKeyData().isHasAlternativeId(keyId))
              .findFirst();
      if (key.isPresent()) {
        selectedRecipients.getList().add(key.get());
      } else {
        missedKeys.add(keyId);
      }
    }
    return missedKeys;
  }

  private void notifyUserOfMissingKeysIfAny(Set<String> missedKeys) {
    if (CollectionUtils.isEmpty(missedKeys)) {
      return;
    }

    UiUtils.messageBox(
        originAction,
        text("error.notAllRecipientsAvailable", List.of(missedKeys)),
        text("term.attention"),
        JOptionPane.WARNING_MESSAGE);
  }

  private boolean doWeHaveKeysToEncryptWith() {
    if (!keyRingService.readKeys().isEmpty()) {
      return true;
    }
    UiUtils.messageBox(
        originAction,
        text("phrase.noKeysForEncryption"),
        text("term.attention"),
        MessageSeverity.WARNING);
    host.getActionToOpenCertificatesList().actionPerformed(originAction);
    if (keyRingService.readKeys().isEmpty()) {
      return false;
    }
    return true;
  }

  private void initModelProperties() {
    List<Key> allKeys = keyRingService.readKeys();
    allKeys.sort(new ComparatorKeyByNameImpl());
    availabileRecipients =
        new ModelListProperty<>(
            this, new ValueAdapterReadonlyImpl<>(allKeys), "availabileRecipients");
    selectedRecipients =
        new ModelMultiSelInListProperty<>(
            this,
            new ValueAdapterHolderImpl<>(new ArrayList<>()),
            "projects",
            availabileRecipients);
    selectedRecipients
        .getModelMultiSelInListPropertyAccessor()
        .addListExEventListener(onRecipientsSelectionChanged);
    onRecipientsSelectionChanged.onListChanged();

    sourceText = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "textToEncrypt");
    sourceText.getModelPropertyAccessor().addPropertyChangeListener(onSourceTextChanged);
    actionDoOperation.setEnabled(false);
    targetText = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "encryptedText");
    targetText.getModelPropertyAccessor().addPropertyChangeListener(onTargetTextChanged);
    actionCopyTargetToClipboard.setEnabled(false);
  }

  private final PropertyChangeListener onSourceTextChanged =
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          refreshPrimaryOperationAvailability();
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

  protected void refreshPrimaryOperationAvailability() {
    boolean result = true;
    result &= selectedRecipients != null && !selectedRecipients.getList().isEmpty();
    result &= sourceText != null && StringUtils.hasText(sourceText.getValue());
    actionDoOperation.setEnabled(result);
  }

  private final ListChangeListenerAnyEventImpl<Key> onRecipientsSelectionChanged =
      new ListChangeListenerAnyEventImpl<>() {
        @Override
        public void onListChanged() {
          refreshPrimaryOperationAvailability();
        }
      };

  protected final Action actionDoOperation =
      new LocalizedActionEx("action.encrypt", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          try {
            String encryptedTextStr =
                encryptionService.encryptText(sourceText.getValue(), selectedRecipients.getList());
            usageLogger.write(
                new EncryptTextRecipientsUsage(
                    selectedRecipients.getList().stream()
                        .map(x -> x.getKeyInfo().getKeyId())
                        .collect(Collectors.toSet())));
            targetText.setValueByOwner(encryptedTextStr);
          } catch (Throwable t) {
            EntryPoint.reportExceptionToUser(e, "error.failedToEncryptText", t);
          }
        }
      };

  protected final Action actionSelectRecipientsFromClipboard =
      new LocalizedActionEx("action.selectKeysFromClipboard", this) {
        @Override
        public Object getValue(String key) {
          if (Action.SHORT_DESCRIPTION.equals(key)) {
            return text("action.selectKeysFromClipboard.tooltip");
          }
          return super.getValue(key);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          String clipboard = ClipboardUtil.tryGetClipboardText();
          if (clipboard == null) {
            UiUtils.messageBox(
                e,
                text("warning.noTextInClipboard"),
                text("term.attention"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
          }

          List<String> emails = findEmailsInText(clipboard);
          if (emails == null) {
            UiUtils.messageBox(
                e,
                text("warning.noEmailsFoundInClipboard"),
                text("term.attention"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
          }

          Set<String> missedKeys = selectRecipientsByEmails(new HashSet<>(emails));
          notifyUserOfMissingKeysIfAny(missedKeys, e);
        }

        private Set<String> selectRecipientsByEmails(Set<String> emails) {
          selectedRecipients.getList().clear();
          Set<String> missedEmails = new HashSet<>();
          for (String email : emails) {
            // TBD: Refactor to have a better way of handling emails. Current
            // implementation is not 100% thorough. It's vulnerable to partial matches and
            // depends on an assumption that email is a part of user name
            Optional<Key> key =
                availabileRecipients.getList().stream()
                    .filter(x -> x.getKeyInfo().getUser().contains(email))
                    .findFirst();
            if (key.isPresent()) {
              selectedRecipients.getList().add(key.get());
            } else {
              missedEmails.add(email);
            }
          }
          return missedEmails;
        }

        private void notifyUserOfMissingKeysIfAny(Set<String> missedKeys, ActionEvent e) {
          if (CollectionUtils.isEmpty(missedKeys)) {
            return;
          }

          UiUtils.messageBox(
              e,
              text("error.notAllEmailsAvailable", List.of(missedKeys)),
              text("term.attention"),
              JOptionPane.WARNING_MESSAGE);
        }
      };

  protected List<String> findEmailsInText(String text) {
    if (!StringUtils.hasText(text)) {
      return null;
    }

    List<String> ret = new ArrayList<>();
    String[] tokens = text.split("[ <>;,]");
    for (String token : tokens) {
      if (ValidationContext.isValidEmail(token)) {
        ret.add(token);
      }
    }

    if (ret.isEmpty()) {
      return null;
    }
    return ret;
  }

  protected final Action actionPasteSourceFromClipboard =
      new LocalizedActionEx("action.pasteFromClipboard", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          String clipboard = ClipboardUtil.tryGetClipboardText();
          if (clipboard == null) {
            UiUtils.messageBox(
                e,
                text("warning.noTextInClipboard"),
                text("term.attention"),
                JOptionPane.INFORMATION_MESSAGE);
            return;
          }

          sourceText.setValueByOwner(clipboard);
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

  protected final Action actionClose =
      new LocalizedActionEx("action.close", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          host.handleClose();
        }
      };

  public ModelMultiSelInListProperty<Key> getSelectedRecipients() {
    return selectedRecipients;
  }

  public ModelPropertyAccessor<String> getSourceText() {
    return sourceText.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getTargetText() {
    return targetText.getModelPropertyAccessor();
  }
}
