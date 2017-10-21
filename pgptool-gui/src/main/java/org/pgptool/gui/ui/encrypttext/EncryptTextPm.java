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
package org.pgptool.gui.ui.encrypttext;

import static org.pgptool.gui.app.Messages.text;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.tools.ClipboardUtil;
import org.pgptool.gui.ui.keyslist.ComparatorKeyByNameImpl;
import org.pgptool.gui.ui.tools.ListChangeListenerAnyEventImpl;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.summerb.approaches.validation.ValidationContext;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.lists.ModelListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultSelInListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultSelInListPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class EncryptTextPm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(EncryptTextPm.class);

	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	@Autowired
	@Resource(name = "encryptionService")
	private EncryptionService<KeyData> encryptionService;

	private EncryptTextHost host;

	private ModelMultSelInListProperty<Key<KeyData>> selectedRecipients;
	private ModelListProperty<Key<KeyData>> availabileRecipients;

	private ModelProperty<String> sourceText;
	private ModelProperty<String> targetText;

	public boolean init(EncryptTextHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		if (!doWeHaveKeysToEncryptWith()) {
			return false;
		}

		initModelProperties();

		return true;
	}

	private boolean doWeHaveKeysToEncryptWith() {
		if (!keyRingService.readKeys().isEmpty()) {
			return true;
		}
		UiUtils.messageBox(text("phrase.noKeysForEncryption"), text("term.attention"), MessageSeverity.WARNING);
		host.getActionToOpenCertificatesList().actionPerformed(null);
		if (keyRingService.readKeys().isEmpty()) {
			return false;
		}
		return true;
	}

	private void initModelProperties() {
		List<Key<KeyData>> allKeys = keyRingService.readKeys();
		allKeys.sort(new ComparatorKeyByNameImpl<>());
		availabileRecipients = new ModelListProperty<Key<KeyData>>(this,
				new ValueAdapterReadonlyImpl<List<Key<KeyData>>>(allKeys), "availabileRecipients");
		selectedRecipients = new ModelMultSelInListProperty<Key<KeyData>>(this,
				new ValueAdapterHolderImpl<List<Key<KeyData>>>(new ArrayList<Key<KeyData>>()), "projects",
				availabileRecipients);
		selectedRecipients.getModelMultSelInListPropertyAccessor().addListExEventListener(onRecipientsSelectionChanged);
		onRecipientsSelectionChanged.onListChanged();

		sourceText = new ModelProperty<>(this, new ValueAdapterHolderImpl<String>(""), "textToEncrypt");
		targetText = new ModelProperty<>(this, new ValueAdapterHolderImpl<String>(""), "encryptedText");
		targetText.getModelPropertyAccessor().addPropertyChangeListener(onTargetTextChanged);
		actionCopyTargetToClipboard.setEnabled(false);
	}

	private PropertyChangeListener onTargetTextChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			boolean hasText = StringUtils.hasText((String) evt.getNewValue());
			actionCopyTargetToClipboard.setEnabled(hasText);
		}
	};

	protected void refreshPrimaryOperationAvailability() {
		boolean result = true;
		result &= !selectedRecipients.getList().isEmpty();
		actionDoOperation.setEnabled(result);
	}

	private ListChangeListenerAnyEventImpl<Key<KeyData>> onRecipientsSelectionChanged = new ListChangeListenerAnyEventImpl<Key<KeyData>>() {
		@Override
		public void onListChanged() {
			refreshPrimaryOperationAvailability();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionDoOperation = new LocalizedAction("action.encrypt") {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				String encryptedTextStr = encryptionService.encryptText(sourceText.getValue(),
						selectedRecipients.getList());
				targetText.setValueByOwner(encryptedTextStr);
			} catch (Throwable t) {
				log.error("Failed to encrypt", t);
				EntryPoint.reportExceptionToUser("error.failedToEncryptText", t);
				return;
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionSelectRecipientsFromClipboard = new LocalizedAction("action.selectKeysFromClipboard") {
		@Override
		public Object getValue(String key) {
			if (Action.SHORT_DESCRIPTION.equals(key)) {
				return text("action.selectKeysFromClipboard.tooltip");
			}
			return super.getValue(key);
		};

		@Override
		public void actionPerformed(ActionEvent e) {
			String clipboard = ClipboardUtil.tryGetClipboardText();
			if (clipboard == null) {
				UiUtils.messageBox(findRegisteredWindowIfAny(), text("warning.noTextInClipboard"),
						text("term.attention"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			List<String> emails = findEmailsInText(clipboard);
			if (emails == null) {
				UiUtils.messageBox(findRegisteredWindowIfAny(), text("warning.noEmailsFoundInClipboard"),
						text("term.attention"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			Set<String> missedKeys = selectRecipients(new HashSet<>(emails));
			notifyUserOfMissingKeysIfAny(missedKeys);
		}

		private Set<String> selectRecipients(Set<String> emails) {
			selectedRecipients.getList().clear();
			Set<String> missedEmails = new HashSet<>();
			for (String email : emails) {
				// TODO: Refactor to have a better way of handling emails. Current
				// implementation is not 100% thorough. It's vulnerable to partial matches and
				// depends on an assumption that email is a part of user name
				Optional<Key<KeyData>> key = availabileRecipients.getList().stream()
						.filter(x -> x.getKeyInfo().getUser().contains(email)).findFirst();
				if (key.isPresent()) {
					selectedRecipients.getList().add(key.get());
				} else {
					missedEmails.add(email);
				}
			}
			return missedEmails;
		}

		private void notifyUserOfMissingKeysIfAny(Set<String> missedKeys) {
			if (CollectionUtils.isEmpty(missedKeys)) {
				return;
			}

			UiUtils.messageBox(findRegisteredWindowIfAny(),
					text("error.notAllEmailsAvailable", Arrays.asList(missedKeys)), text("term.attention"),
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

		if (ret.size() == 0) {
			return null;
		}
		return ret;
	}

	@SuppressWarnings("serial")
	protected final Action actionPasteSourceFromClipboard = new LocalizedAction("action.pasteFromClipboard") {
		@Override
		public void actionPerformed(ActionEvent e) {
			String clipboard = ClipboardUtil.tryGetClipboardText();
			if (clipboard == null) {
				UiUtils.messageBox(findRegisteredWindowIfAny(), text("warning.noTextInClipboard"),
						text("term.attention"), JOptionPane.INFORMATION_MESSAGE);
				return;
			}

			sourceText.setValueByOwner(clipboard);
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
	protected final Action actionClose = new LocalizedAction("action.close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
		}
	};

	public ModelMultSelInListPropertyAccessor<Key<KeyData>> getSelectedRecipients() {
		return selectedRecipients.getModelMultSelInListPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getSourceText() {
		return sourceText.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getTargetText() {
		return targetText.getModelPropertyAccessor();
	}

}
