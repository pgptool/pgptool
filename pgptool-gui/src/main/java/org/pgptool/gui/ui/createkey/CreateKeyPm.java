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
package org.pgptool.gui.ui.createkey;

import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.swing.Action;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.hintsforusage.hints.PrivateKeyBackupHint.KeyCreatedEvent;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.usage.api.UsageLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.summerb.easycrud.api.dto.EntityChangedEvent;
import org.summerb.validation.FieldValidationException;
import org.summerb.validation.ValidationError;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

import ru.skarpushin.swingpm.EXPORT.base.LocalizedActionEx;
import ru.skarpushin.swingpm.EXPORT.base.PresentationModelBase;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ConversionValueAdapter;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReflectionImpl;

public class CreateKeyPm extends PresentationModelBase<CreateKeyHost, Void> {
	private static Logger log = Logger.getLogger(CreateKeyPm.class);

	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private KeyGeneratorService keyGeneratorService;
	@Autowired
	private ExecutorService executorService;
	@Autowired
	private EventBus eventBus;
	@Autowired
	private UsageLogger usageLogger;

	private CreateKeyParams createKeyParams = new CreateKeyParams();

	private ModelProperty<String> fullName;
	private ModelProperty<String> email;
	private ModelProperty<String> passphrase;
	private ModelProperty<String> passphraseAgain;
	private ModelProperty<Boolean> isDisableControls;

	private ModelProperty<Boolean> progressBarVisible;

	private ListEx<ValidationError> validationErrors = new ListExImpl<ValidationError>();

	protected Future<?> keyGenerationFuture;

	@Override
	public boolean init(ActionEvent originAction, CreateKeyHost host, Void initParams) {
		super.init(originAction, host, initParams);
		Preconditions.checkArgument(host != null);
		initModelProperties();
		keyGeneratorService.expectNewKeyCreation();
		return true;
	}

	private void initModelProperties() {
		fullName = initStringModelProp(CreateKeyParams.FN_FULL_NAME);
		email = initStringModelProp(CreateKeyParams.FN_EMAIL);
		passphrase = initStringModelProp(CreateKeyParams.FN_PASSPHRASE);
		passphraseAgain = initStringModelProp(CreateKeyParams.FN_PASSPHRASE_AGAIN);

		isDisableControls = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isDisableControls");
		progressBarVisible = new ModelProperty<>(this, new ValueAdapterHolderImpl<Boolean>(false),
				"progressBarVisible");
	}

	private ModelProperty<String> initStringModelProp(String fieldName) {
		return new ModelProperty<String>(this,
				new NullToEmptyStringConverter(new ValueAdapterReflectionImpl<String>(createKeyParams, fieldName)),
				fieldName, validationErrors);
	}

	@SuppressWarnings("serial")
	protected Action actionCreate = new LocalizedActionEx("action.create", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			progressBarVisible.setValueByOwner(true);
			actionCreate.setEnabled(false);
			isDisableControls.setValueByOwner(true);
			keyGenerationFuture = executorService.submit(new KeyGenerationRunnable(e));
		}
	};

	private class KeyGenerationRunnable implements Runnable {
		private ActionEvent runnableOriginEvent;

		public KeyGenerationRunnable(ActionEvent runnableOriginEvent) {
			this.runnableOriginEvent = runnableOriginEvent;
		}

		@Override
		public void run() {
			validationErrors.clear();
			try {
				usageLogger.write(new CreateKeyUsage(createKeyParams.getFullName(), createKeyParams.getEmail()));
				boolean emptyPassphraseConsent = !StringUtils.hasText(createKeyParams.getPassphrase())
						&& UiUtils.confirmWarning(runnableOriginEvent, "confirm.createKeyWithoutPassphrase", null);
				Key key = keyGeneratorService.createNewKey(createKeyParams, emptyPassphraseConsent);
				if (keyGenerationFuture == null) {
					return; // this will happen if user canceled operation
				}
				keyRingService.addKey(key);
				eventBus.post(EntityChangedEvent.added(new KeyCreatedEvent(key)));
				usageLogger.write(new KeyCreatedUsage(key.getKeyInfo()));
				host.handleClose();
			} catch (FieldValidationException fve) {
				validationErrors.addAll(fve.getErrors());
			} catch (Throwable t) {
				log.error("Failed to create key", t);
				EntryPoint.reportExceptionToUser(runnableOriginEvent, "exception.failedToCreatePgpKey", t);
			} finally {
				progressBarVisible.setValueByOwner(false);
				actionCreate.setEnabled(true);
				isDisableControls.setValueByOwner(false);
			}
		}
	}

	@SuppressWarnings("serial")
	protected Action actionCancel = new LocalizedActionEx("action.cancel", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			Future<?> tmp = keyGenerationFuture;
			keyGenerationFuture = null;
			if (tmp != null) {
				tmp.cancel(true);
			}

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

	public ModelPropertyAccessor<Boolean> getIsDisableControls() {
		return isDisableControls.getModelPropertyAccessor();
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
