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
package org.pgptool.gui.ui.changekeyuserid;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import javax.swing.Action;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.ChangeUserIdParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.ui.createkey.NullToEmptyStringConverter;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.springframework.util.StringUtils;
import org.summerb.methodCapturers.PropertyNameResolver;
import org.summerb.methodCapturers.PropertyNameResolverFactory;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationException;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReflectionImpl;

public class ChangeKeyUserIdPm extends PresentationModelBaseEx<ChangeKeyUserIdHost, Key> {
  private final KeyRingService keyRingService;
  private final KeyGeneratorService keyGeneratorService;
  private final KeyFilesOperations keyFilesOperations;
  private final PropertyNameResolverFactory propertyNameResolverFactory;

  private Key key;

  private final ChangeUserIdParams params = new ChangeUserIdParams();
  private ModelProperty<String> currentUser;
  private ModelProperty<String> fullName;
  private ModelProperty<String> email;
  private ModelProperty<String> passphrase;

  private final ListEx<ValidationError> validationErrors = new ListExImpl<>();

  public ChangeKeyUserIdPm(
      KeyRingService keyRingService,
      KeyGeneratorService keyGeneratorService,
      KeyFilesOperations keyFilesOperations,
      PropertyNameResolverFactory propertyNameResolverFactory) {
    this.keyRingService = keyRingService;
    this.keyGeneratorService = keyGeneratorService;
    this.keyFilesOperations = keyFilesOperations;
    this.propertyNameResolverFactory = propertyNameResolverFactory;
  }

  @Override
  public boolean init(ActionEvent originAction, ChangeKeyUserIdHost host, Key key) {
    super.init(originAction, host, initParams);
    Preconditions.checkArgument(host != null);
    Preconditions.checkArgument(key != null, "key is required");
    this.key = key;
    initModelProperties();
    return true;
  }

  private void initModelProperties() {
    PropertyNameResolver<ChangeUserIdParams> nameResolver =
        propertyNameResolverFactory.getResolver(ChangeUserIdParams.class);

    currentUser =
        new ModelProperty<>(
            this, new ValueAdapterReadonlyImpl<>(key.getKeyInfo().getUser()), "currentUser");
    passphrase =
        new ModelProperty<>(
            this,
            new NullToEmptyStringConverter(
                new ValueAdapterReflectionImpl<>(
                    params, nameResolver.resolve(ChangeUserIdParams::getPassphrase))),
            "passphrase",
            validationErrors);
    fullName =
        new ModelProperty<>(
            this,
            new NullToEmptyStringConverter(
                new ValueAdapterReflectionImpl<>(
                    params, nameResolver.resolve(ChangeUserIdParams::getFullName))),
            "fullName",
            validationErrors);
    email =
        new ModelProperty<>(
            this,
            new NullToEmptyStringConverter(
                new ValueAdapterReflectionImpl<>(
                    params, nameResolver.resolve(ChangeUserIdParams::getEmail))),
            "email",
            validationErrors);
  }

  protected final Action actionChange =
      new LocalizedActionEx("action.ok", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          actionChange.setEnabled(false);
          validationErrors.clear();
          try {
            // Validate current passphrase
            keyFilesOperations.validateKeyPassword(key, params.getPassphrase());

            if (!StringUtils.hasText(fullName.getValue())) {
              UiUtils.messageBox(e, "Full name is required", "", 2);
              return;
            }

            Key newKey = keyGeneratorService.replacePrimaryUserId(key, params);
            if (newKey == null) {
              return;
            }
            keyRingService.replaceKey(key, newKey);
            host.handleClose();
          } catch (ValidationException vex) {
            validationErrors.addAll(vex.getErrors());
          } catch (Throwable t) {
            EntryPoint.reportExceptionToUser(e, "exception.failedToChangeKey", t);
          } finally {
            actionChange.setEnabled(true);
          }
        }
      };

  protected final Action actionCancel =
      new LocalizedActionEx("action.cancel", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          host.handleClose();
        }
      };

  public ModelPropertyAccessor<String> getCurrentUser() {
    return currentUser.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getFullName() {
    return fullName.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getEmail() {
    return email.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getPassphrase() {
    return passphrase.getModelPropertyAccessor();
  }
}
