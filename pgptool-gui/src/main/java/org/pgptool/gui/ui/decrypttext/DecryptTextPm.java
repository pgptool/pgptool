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
package org.pgptool.gui.ui.decrypttext;

import static org.pgptool.gui.app.Messages.text;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
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
import org.pgptool.gui.encryption.implpgp.SymmetricEncryptionIsNotSupportedException;
import org.pgptool.gui.tools.ClipboardUtil;
import org.pgptool.gui.ui.decryptonedialog.KeyAndPasswordCallback;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class DecryptTextPm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(DecryptTextPm.class);

	@Autowired
	// @Resource(name = "keyRingService")
	private KeyRingService keyRingService;
	@Autowired
	// @Resource(name = "encryptionService")
	private EncryptionService encryptionService;
	@Autowired

	private DecryptTextHost host;

	private Set<String> keysIds;
	private PasswordDeterminedForKey keyAndPassword;

	private ModelProperty<String> sourceText;
	private ModelProperty<String> recipients;
	protected Set<String> recipientsList;
	private ModelProperty<String> targetText;

	public boolean init(DecryptTextHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		if (!doWeHaveKeysToDecryptWith()) {
			return false;
		}

		initModelProperties();

		// TODO: See if there is already text in clipboard that we can decrypt
		// if (StringUtils.hasText(ClipboardUtil.tryGetClipboardText())) {
		// if (!pasteFromCLipboardAndTryDecrypt()) {
		// return false;
		// }
		// }
		// NOTE: At this moment this feature is commented out because we have a problem
		// with dialogs display order -- when password dialog displayed before main
		// dialog it then gets covered by latter

		return true;
	}

	private void initModelProperties() {
		sourceText = new ModelProperty<>(this, new ValueAdapterHolderImpl<String>(""), "textToEncrypt");
		sourceText.getModelPropertyAccessor().addPropertyChangeListener(onSourceTextChanged);
		actionDecrypt.setEnabled(false);

		targetText = new ModelProperty<>(this, new ValueAdapterHolderImpl<String>(""), "encryptedText");
		targetText.getModelPropertyAccessor().addPropertyChangeListener(onTargetTextChanged);
		actionCopyTargetToClipboard.setEnabled(false);

		recipients = new ModelProperty<>(this, new ValueAdapterHolderImpl<String>(""), "recipients");
	}

	private PropertyChangeListener onSourceTextChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			actionDecrypt.setEnabled(StringUtils.hasText((String) evt.getNewValue()));
		}
	};

	private PropertyChangeListener onTargetTextChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			boolean hasText = StringUtils.hasText((String) evt.getNewValue());
			actionCopyTargetToClipboard.setEnabled(hasText);
		}
	};

	private boolean doWeHaveKeysToDecryptWith() {
		if (!keyRingService.readKeys().isEmpty()) {
			return true;
		}
		UiUtils.messageBox(text("phrase.noKeysForDecryption"), text("term.attention"), MessageSeverity.WARNING);
		host.getActionToOpenCertificatesList().actionPerformed(null);
		if (keyRingService.readKeys().isEmpty()) {
			return false;
		}
		return true;
	}

	public ModelPropertyAccessor<String> getSourceText() {
		return sourceText.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getTargetText() {
		return targetText.getModelPropertyAccessor();
	}

	private void decrypt() throws UnsupportedEncodingException, SymmetricEncryptionIsNotSupportedException {
		// Discover possible keys
		ByteArrayInputStream inputStream = new ByteArrayInputStream(sourceText.getValue().getBytes("UTF-8"));
		keysIds = encryptionService.findKeyIdsForDecryption(inputStream);

		// Show list of emails
		recipientsList = new HashSet<>(keysIds);
		recipients.setValueByOwner(collectRecipientsNames(keysIds));

		// Request password for decryption key
		if (keyAndPassword != null && keysIds.contains(keyAndPassword.getDecryptionKeyId())) {
			keyAndPasswordCallback.onKeyPasswordResult(keyAndPassword);
		} else {
			Message purpose = new Message("phrase.needKeyToDecryptFromClipboard");
			host.askUserForKeyAndPassword(keysIds, purpose, keyAndPasswordCallback, findRegisteredWindowIfAny());
		}
	}

	private String pasteFromCLipboard() {
		String clipboard = ClipboardUtil.tryGetClipboardText();
		if (clipboard == null || !StringUtils.hasText(clipboard)) {
			UiUtils.messageBox(findRegisteredWindowIfAny(), text("warning.noTextInClipboard"), text("term.attention"),
					JOptionPane.INFORMATION_MESSAGE);
			return null;
		}
		sourceText.setValueByOwner(clipboard);
		return clipboard;
	}

	private String collectRecipientsNames(Set<String> keysIds) {
		StringBuilder ret = new StringBuilder();
		for (String keyId : keysIds) {
			if (ret.length() > 0) {
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

	private KeyAndPasswordCallback keyAndPasswordCallback = new KeyAndPasswordCallback() {
		@Override
		public void onKeyPasswordResult(PasswordDeterminedForKey keyAndPassword) {
			try {
				DecryptTextPm.this.keyAndPassword = keyAndPassword;
				if (keyAndPassword == null) {
					UiUtils.messageBox(text("error.noMatchingKeysRegistered"), text("term.error"),
							MessageSeverity.ERROR);
					return;
				}

				// Decrypt
				String decryptedText = encryptionService.decryptText(sourceText.getValue(), keyAndPassword);

				// Set target text
				targetText.setValueByOwner(decryptedText);
			} catch (Throwable t) {
				log.error("Failed to decrypt text", t);
				EntryPoint.reportExceptionToUser("error.cantParseEncryptedText", t);
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionReply = new LocalizedAction("action.reply") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
			host.openEncryptText(recipientsList);
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionCopyTargetToClipboard = new LocalizedAction("action.copyToClipboard") {
		@Override
		public void actionPerformed(ActionEvent e) {
			ClipboardUtil.setClipboardText(targetText.getValue());
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionPasteAndDecrypt = new LocalizedAction("action.decryptFromClipboard") {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				String clipboard = pasteFromCLipboard();
				if (clipboard == null) {
					return;
				}
				decrypt();
			} catch (Throwable t) {
				log.info("Failed to process text", t);
				EntryPoint.reportExceptionToUser("error.cantParseEncryptedText", t);
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionDecrypt = new LocalizedAction("action.decrypt") {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Preconditions.checkState(StringUtils.hasText(sourceText.getValue()),
						"Action must not be invoked if source text is empty");
				decrypt();
			} catch (Throwable t) {
				log.info("Failed to process text", t);
				EntryPoint.reportExceptionToUser("error.cantParseEncryptedText", t);
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionCancel = new LocalizedAction("action.close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
		}
	};
}
