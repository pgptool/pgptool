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
package org.pgptool.gui.ui.createkey;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.swing.Action;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationError;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ConversionValueAdapter;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReflectionImpl;

public class CreateKeyPm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(CreateKeyPm.class);

	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	@Autowired
	@Resource(name = "keyGeneratorService")
	private KeyGeneratorService<KeyData> keyGeneratorService;
	@Autowired
	private ExecutorService executorService;

	private CreateKeyHost host;

	private CreateKeyParams createKeyParams = new CreateKeyParams();

	private ModelProperty<String> fullName;
	private ModelProperty<String> email;
	private ModelProperty<String> passphrase;
	private ModelProperty<String> passphraseAgain;

	private ModelProperty<Boolean> progressBarVisible;

	private ListEx<ValidationError> validationErrors = new ListExImpl<ValidationError>();

	protected Future<?> keyGenerationFuture;

	public void init(CreateKeyHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		initModelProperties();
	}

	private void initModelProperties() {
		fullName = initStringModelProp(CreateKeyParams.FN_FULL_NAME);
		email = initStringModelProp(CreateKeyParams.FN_EMAIL);
		passphrase = initStringModelProp(CreateKeyParams.FN_PASSPHRASE);
		passphraseAgain = initStringModelProp(CreateKeyParams.FN_PASSPHRASE_AGAIN);

		progressBarVisible = new ModelProperty<>(this, new ValueAdapterHolderImpl<Boolean>(false),
				"progressBarVisible");
	}

	private ModelProperty<String> initStringModelProp(String fieldName) {
		return new ModelProperty<String>(this,
				new NullToEmptyStringConverter(new ValueAdapterReflectionImpl<String>(createKeyParams, fieldName)),
				fieldName, validationErrors);
	}

	@SuppressWarnings("serial")
	protected Action actionCreate = new LocalizedAction("action.create") {
		@Override
		public void actionPerformed(ActionEvent e) {
			progressBarVisible.setValueByOwner(true);
			actionCreate.setEnabled(false);
			keyGenerationFuture = executorService.submit(new KeyGenerationRunnable());
		}
	};

	private class KeyGenerationRunnable implements Runnable {
		@Override
		public void run() {
			validationErrors.clear();
			try {
				Key<KeyData> key = keyGeneratorService.createNewKey(createKeyParams);
				if (keyGenerationFuture == null) {
					return;
				}
				keyRingService.addKey(key);
				host.handleClose();
			} catch (FieldValidationException fve) {
				validationErrors.addAll(fve.getErrors());
			} catch (Throwable t) {
				log.error("Failed to create key", t);
				EntryPoint.reportExceptionToUser("exception.failedToCreatePgpKey", t);
			} finally {
				progressBarVisible.setValueByOwner(false);
				actionCreate.setEnabled(true);
			}
		}
	}

	@SuppressWarnings("serial")
	protected Action actionCancel = new LocalizedAction("action.cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			Future<?> tmp = keyGenerationFuture;
			keyGenerationFuture = null;
			tmp.cancel(true);

			host.handleClose();
		}
	};

	public ModelPropertyAccessor<String> getFullName() {
		return fullName.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getEmail() {
		return email.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getPassphrase() {
		return passphrase.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getPassphraseAgain() {
		return passphraseAgain.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getProgressBarVisible() {
		return progressBarVisible.getModelPropertyAccessor();
	}

	static class NullToEmptyStringConverter extends ConversionValueAdapter<String, String> {
		public NullToEmptyStringConverter(ValueAdapter<String> innerValueAdapter) {
			super(innerValueAdapter);
		}

		@Override
		protected String convertInnerToOuter(String value) {
			return value != null ? value : "";
		}

		@Override
		protected String convertOuterToInner(String value) {
			return value != null ? value : "";
		}
	}
}
