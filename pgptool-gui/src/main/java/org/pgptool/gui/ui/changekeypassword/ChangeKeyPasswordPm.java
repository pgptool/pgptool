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
package org.pgptool.gui.ui.changekeypassword;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.ChangePasswordParams;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.ui.createkey.NullToEmptyStringConverter;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.summerb.validation.FieldValidationException;
import org.summerb.validation.ValidationError;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReflectionImpl;

public class ChangeKeyPasswordPm extends PresentationModelBaseEx<ChangeKeyPasswordHost, Key> {
	private static Logger log = Logger.getLogger(ChangeKeyPasswordPm.class);

	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private KeyGeneratorService keyGeneratorService;
	@Autowired
	private KeyFilesOperations keyFilesOperations;

	private Key key;

	private ChangePasswordParams changePasswordParams = new ChangePasswordParams();
	private ModelProperty<String> fullName;
	private ModelProperty<String> passphrase;
	private ModelProperty<String> newPassphrase;
	private ModelProperty<String> newPassphraseAgain;

	private ListEx<ValidationError> validationErrors = new ListExImpl<ValidationError>();

	@Override
	public boolean init(ActionEvent originAction, ChangeKeyPasswordHost host, Key key) {
		super.init(originAction, host, initParams);
		Preconditions.checkArgument(host != null);
		Preconditions.checkArgument(key != null, "key is required");
		this.key = key;
		initModelProperties();
		return true;
	}

	private void initModelProperties() {
		fullName = new ModelProperty<>(this, new ValueAdapterReadonlyImpl<>(key.getKeyInfo().getUser()),
				CreateKeyParams.FN_FULL_NAME);

		passphrase = initStringModelProp(CreateKeyParams.FN_PASSPHRASE);
		newPassphrase = initStringModelProp(ChangePasswordParams.FN_NEW_PASSPHRASE);
		newPassphraseAgain = initStringModelProp(ChangePasswordParams.FN_NEW_PASSPHRASE_AGAIN);
	}

	private ModelProperty<String> initStringModelProp(String fieldName) {
		return new ModelProperty<String>(this,
				new NullToEmptyStringConverter(new ValueAdapterReflectionImpl<String>(changePasswordParams, fieldName)),
				fieldName, validationErrors);
	}

	@SuppressWarnings("serial")
	protected Action actionChange = new LocalizedActionEx("action.ok", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			actionChange.setEnabled(false);
			validationErrors.clear();
			try {
				keyFilesOperations.validateKeyPassword(key, changePasswordParams.getPassphrase());

				boolean emptyPassphraseConsent = !StringUtils.hasText(changePasswordParams.getNewPassphrase())
						&& UiUtils.confirmWarning(e, "confirm.createKeyWithoutPassphrase", null);

				Key newKey;
				if ((newKey = keyGeneratorService.changeKeyPassword(key, changePasswordParams,
						emptyPassphraseConsent)) == null) {
					return;
				}

				keyRingService.replaceKey(key, newKey);

				// x.
				host.handleClose();
			} catch (FieldValidationException fve) {
				validationErrors.addAll(fve.getErrors());
			} catch (Throwable t) {
				log.error("Failed to change key password", t);
				EntryPoint.reportExceptionToUser(e, "exception.failedToChangeKey", t);
			} finally {
				actionChange.setEnabled(true);
			}
		}
	};

	@SuppressWarnings("serial")
	protected Action actionCancel = new LocalizedActionEx("action.cancel", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			host.handleClose();
		}
	};

	public ModelPropertyAccessor<String> getFullName() {
		return fullName.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getPassphrase() {
		return passphrase.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getNewPassphrase() {
		return newPassphrase.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getNewPassphraseAgain() {
		return newPassphraseAgain.getModelPropertyAccessor();
	}
}
