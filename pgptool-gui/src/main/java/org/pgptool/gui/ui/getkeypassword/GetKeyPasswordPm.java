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
package org.pgptool.gui.ui.getkeypassword;

import static org.pgptool.gui.app.Messages.text;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Action;

import org.pgptool.gui.app.Message;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.MatchedKey;
import org.pgptool.gui.usage.api.KeyUsage;
import org.pgptool.gui.usage.api.UsageLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.summerb.validation.FieldValidationException;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationErrorsUtils;
import org.summerb.validation.errors.FieldRequiredValidationError;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.EXPORT.base.LocalizedActionEx;
import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.lists.ModelListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelSelInComboBoxProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelSelInComboBoxPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

/**
 * This component is designed to ask user to choose key and provide password for
 * it. Component will respond to host using
 * {@link GetKeyPasswordHost#onPasswordDeterminedForKey(PasswordDeterminedForKey)}
 * method.
 * 
 * It will also cache passwords and if password was found in cache then thre
 * will be no need to show UI again.
 * 
 * @author Sergey Karpushin
 *
 */
public class GetKeyPasswordPm extends PresentationModelBase {
	private static final String FN_PASSWORD = "password";
	private static final Map<String, PasswordDeterminedForKey> CACHE_KEYID_TO_PASSWORD = new HashMap<>();

	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private KeyFilesOperations keyFilesOperations;
	@Autowired
	private EventBus eventBus;
	@Autowired
	private UsageLogger usageLogger;

	private GetKeyPasswordHost host;

	private ModelSelInComboBoxProperty<Key> selectedKey;
	private ModelListProperty<Key> decryptionKeys;
	private ModelProperty<String> password;
	private ModelProperty<String> purpose;
	private ListEx<ValidationError> validationErrors = new ListExImpl<ValidationError>();

	private List<MatchedKey> matchedKeys;
	private Message purposeMessage;
	private boolean registeredOnEventBus;

	public GetKeyPasswordPmInitResult init(GetKeyPasswordHost host, Set<String> keyIdsToChooseFrom,
			Message purposeMessage) {
		this.purposeMessage = purposeMessage;
		Preconditions.checkArgument(host != null);
		this.host = host;

		// Fill model with matching keys
		matchedKeys = keyRingService.findMatchingDecryptionKeys(keyIdsToChooseFrom);
		// NOTE: We're assuming here keys are distinct meaning same key will not
		// appear 2 times
		if (matchedKeys.size() == 0) {
			// UiUtils.messageBox(text("error.noMatchingKeysRegistered"),
			// text("term.attention"), MessageSeverity.WARNING);
			return GetKeyPasswordPmInitResult.NoMatchingKeys;
		}

		// If password was cached -- call host immediately
		if (passwordWasCached(host, matchedKeys)) {
			return GetKeyPasswordPmInitResult.CachedPasswordFound;
		}

		initModelProperties(matchedKeys.stream().map(x -> x.getMatchedKey()).collect(Collectors.toList()));
		eventBus.register(this);
		registeredOnEventBus = true;

		// x. ret
		return GetKeyPasswordPmInitResult.ShowUiAndAskUser;
	}

	@Override
	public void detach() {
		super.detach();
		if (registeredOnEventBus) {
			eventBus.unregister(this);
			registeredOnEventBus = false;
		}
	}

	private boolean passwordWasCached(GetKeyPasswordHost host, List<MatchedKey> matchedKeys) {
		for (MatchedKey k : matchedKeys) {
			if (CACHE_KEYID_TO_PASSWORD.containsKey(k.getRequestedKeyId())) {
				host.onPasswordDeterminedForKey(CACHE_KEYID_TO_PASSWORD.get(k.getRequestedKeyId()));
				return true;
			}
		}
		return false;
	}

	private void initModelProperties(List<Key> keys) {
		purpose = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(text(purposeMessage)), "purpose",
				validationErrors);
		decryptionKeys = new ModelListProperty<Key>(this, new ValueAdapterReadonlyImpl<List<Key>>(keys),
				"decryptionKeys");
		selectedKey = new ModelSelInComboBoxProperty<Key>(this, new ValueAdapterHolderImpl<Key>(keys.get(0)),
				"selectedKey", decryptionKeys);
		password = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), FN_PASSWORD, validationErrors);
		password.getModelPropertyAccessor().addPropertyChangeListener(onPasswordChanged);
	}

	private PropertyChangeListener onPasswordChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			validatePassword();
		}

		private void validatePassword() {
			if (StringUtils.hasText(password.getValue())) {
				validationErrors.removeAll(ValidationErrorsUtils.findErrorsForField(FN_PASSWORD, validationErrors));
				// NOTE: We also can try to check password while user is
				// typing... Should we do that? It might be annoying to see red
				// border before user completes writing password. And once hes
				// done he just press enter and finf out whether password was
				// correct
			} else {
				validationErrors.add(new FieldRequiredValidationError(FN_PASSWORD));
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionCancel = new LocalizedActionEx("action.cancel", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			host.onCancel();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionChooseKey = new LocalizedActionEx("action.ok", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			Key key = selectedKey.getValue();
			String passwordStr = password.getValue();

			Optional<MatchedKey> matchedKey = matchedKeys.stream().filter(x -> x.getMatchedKey() == key).findFirst();
			Preconditions.checkState(matchedKey.isPresent(), "Failed to find matching key to key selected in combobox");
			String requestedKeyId = matchedKey.get().getRequestedKeyId();

			try {
				validationErrors.removeAll(ValidationErrorsUtils.findErrorsForField(FN_PASSWORD, validationErrors));
				if (!StringUtils.hasText(password.getValue())) {
					validationErrors.add(new FieldRequiredValidationError(FN_PASSWORD));
					return;
				}

				keyFilesOperations.validateDecryptionKeyPassword(requestedKeyId, key, passwordStr);
			} catch (FieldValidationException fve) {
				validationErrors.addAll(fve.getErrors());
				return;
			}

			// If everything is ok -- return
			PasswordDeterminedForKey ret = new PasswordDeterminedForKey(requestedKeyId, key, passwordStr);
			CACHE_KEYID_TO_PASSWORD.put(requestedKeyId, ret);
			usageLogger.write(new KeyUsage(requestedKeyId));
			eventBus.post(ret);
		}
	};

	@Subscribe
	public void onPasswordProvidedByOtherInstance(PasswordDeterminedForKey evt) {
		for (MatchedKey mk : matchedKeys) {
			if (mk.getRequestedKeyId().equals(evt.getDecryptionKeyId())) {
				host.onPasswordDeterminedForKey(evt);
				return;
			}
		}
	}

	public ModelSelInComboBoxPropertyAccessor<Key> getSelectedKey() {
		return selectedKey.getModelSelInComboBoxPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getPassword() {
		return password.getModelPropertyAccessor();
	}

	public List<MatchedKey> getMatchedKeys() {
		return matchedKeys;
	}

	public ModelPropertyAccessor<String> getPurpose() {
		return purpose.getModelPropertyAccessor();
	}

}
