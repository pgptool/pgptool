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

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import java.awt.event.ActionEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import javax.swing.Action;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.hintsforusage.hints.PrivateKeyBackupHint.KeyCreatedEvent;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.pgptool.gui.usage.api.UsageLogger;
import org.springframework.util.StringUtils;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent;
import org.summerb.utils.exceptions.ExceptionUtils;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationException;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReflectionImpl;

public class CreateKeyPm extends PresentationModelBaseEx<CreateKeyHost, Void> {
  private final KeyRingService keyRingService;
  private final KeyGeneratorService keyGeneratorService;
  private final ExecutorService executorService;
  private final EventBus eventBus;
  private final UsageLogger usageLogger;

  private final CreateKeyParams createKeyParams = new CreateKeyParams();

  private ModelProperty<String> fullName;
  private ModelProperty<String> email;
  private ModelProperty<String> passphrase;
  private ModelProperty<String> passphraseAgain;
  private ModelProperty<Boolean> isDisableControls;

  private ModelProperty<Boolean> progressBarVisible;

  private final ListEx<ValidationError> validationErrors = new ListExImpl<>();

  protected Future<?> keyGenerationFuture;

  public CreateKeyPm(
      KeyRingService keyRingService,
      KeyGeneratorService keyGeneratorService,
      ExecutorService executorService,
      EventBus eventBus,
      UsageLogger usageLogger) {
    this.keyRingService = keyRingService;
    this.keyGeneratorService = keyGeneratorService;
    this.executorService = executorService;
    this.eventBus = eventBus;
    this.usageLogger = usageLogger;
  }

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

    isDisableControls =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isDisableControls");
    progressBarVisible =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "progressBarVisible");
  }

  private ModelProperty<String> initStringModelProp(String fieldName) {
    return new ModelProperty<>(
        this,
        new NullToEmptyStringConverter(
            new ValueAdapterReflectionImpl<>(createKeyParams, fieldName)),
        fieldName,
        validationErrors);
  }

  protected Action actionCreate =
      new LocalizedActionEx("action.create", this) {
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
    private final ActionEvent runnableOriginEvent;

    public KeyGenerationRunnable(ActionEvent runnableOriginEvent) {
      this.runnableOriginEvent = runnableOriginEvent;
    }

    @Override
    public void run() {
      validationErrors.clear();
      try {
        usageLogger.write(
            new CreateKeyUsage(createKeyParams.getFullName(), createKeyParams.getEmail()));
        boolean emptyPassphraseConsent =
            !StringUtils.hasText(createKeyParams.getPassphrase())
                && UiUtils.confirmWarning(
                    runnableOriginEvent, "confirm.createKeyWithoutPassphrase", null);
        Key key = keyGeneratorService.createNewKey(createKeyParams, emptyPassphraseConsent);
        if (keyGenerationFuture == null) {
          return; // this will happen if user canceled operation
        }
        keyRingService.addKey(key);
        eventBus.post(EntityChangedEvent.added(new KeyCreatedEvent(key)));
        usageLogger.write(new KeyCreatedUsage(key.getKeyInfo()));
        host.handleClose();
      } catch (ValidationException fve) {
        validationErrors.addAll(fve.getErrors());
      } catch (Throwable t) {
        if (ExceptionUtils.findExceptionOfType(t, InterruptedException.class) != null) {
          // user cancelled key creation
          host.handleClose();
          return;
        }
        EntryPoint.reportExceptionToUser(runnableOriginEvent, "exception.failedToCreatePgpKey", t);
      } finally {
        progressBarVisible.setValueByOwner(false);
        actionCreate.setEnabled(true);
        isDisableControls.setValueByOwner(false);
      }
    }
  }

  protected Action actionCancel =
      new LocalizedActionEx("action.cancel", this) {
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
}
