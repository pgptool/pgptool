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

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.swing.Action;
import org.apache.log4j.Logger;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.MatchedKey;
import org.pgptool.gui.ui.getkeypassworddialog.GetKeyPasswordDialogPm.GetKeyPasswordPo;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.pgptool.gui.usage.api.KeyUsage;
import org.pgptool.gui.usage.api.UsageLogger;
import org.springframework.util.StringUtils;
import org.summerb.methodCapturers.PropertyNameResolver;
import org.summerb.methodCapturers.PropertyNameResolverFactory;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent.ChangeType;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationErrorsUtils;
import org.summerb.validation.ValidationException;
import org.summerb.validation.errors.MustNotBeNull;
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
 * This component is designed to ask user to choose key and provide password for it. Component will
 * respond to host using {@link
 * GetKeyPasswordHost#onPasswordDeterminedForKey(PasswordDeterminedForKey)} method.
 *
 * <p>It will also cache passwords and if password was found in cache then thre will be no need to
 * show UI again.
 *
 * @author Sergey Karpushin
 */
public class GetKeyPasswordPm
    extends PresentationModelBaseEx<GetKeyPasswordHost, GetKeyPasswordPo> {
  private static final Logger log = Logger.getLogger(GetKeyPasswordPm.class);

  private static final Map<String, PasswordDeterminedForKey> CACHE_KEYID_TO_PASSWORD =
      new HashMap<>();

  private final KeyRingService keyRingService;
  private final KeyFilesOperations keyFilesOperations;
  private final EventBus eventBus;
  private final UsageLogger usageLogger;
  private final PropertyNameResolverFactory propertyNameResolverFactory;

  private ModelSelInComboBoxProperty<Key> selectedKey;
  private ModelListProperty<Key> decryptionKeys;
  private ModelProperty<String> passphrase;
  private ModelProperty<String> purpose;
  private final ListEx<ValidationError> validationErrors = new ListExImpl<>();

  private List<MatchedKey> matchedKeys;
  private boolean registeredOnEventBus;

  public GetKeyPasswordPm(
      KeyRingService keyRingService,
      KeyFilesOperations keyFilesOperations,
      EventBus eventBus,
      UsageLogger usageLogger,
      PropertyNameResolverFactory propertyNameResolverFactory) {
    this.keyRingService = keyRingService;
    this.keyFilesOperations = keyFilesOperations;
    this.eventBus = eventBus;
    this.usageLogger = usageLogger;
    this.propertyNameResolverFactory = propertyNameResolverFactory;
  }

  @Override
  public boolean init(
      ActionEvent originAction, GetKeyPasswordHost host, GetKeyPasswordPo initParams) {
    throw new UnsupportedOperationException(
        "GetKeyPasswordPm is not meant to be initialized by regular init. Use initEx instead");
  }

  public GetKeyPasswordPmInitResult initEx(
      ActionEvent originAction, GetKeyPasswordHost host, GetKeyPasswordPo initParams) {
    super.init(originAction, host, initParams);
    Preconditions.checkArgument(host != null);

    // Fill model with matching keys
    matchedKeys = keyRingService.findMatchingDecryptionKeys(initParams.keysIds);
    // NOTE: We're assuming here keys are distinct meaning same key will not
    // appear 2 times
    if (matchedKeys.isEmpty()) {
      // UiUtils.messageBox(text("error.noMatchingKeysRegistered"),
      // text("term.attention"), MessageSeverity.WARNING);
      return GetKeyPasswordPmInitResult.NoMatchingKeys;
    }

    // If password was cached -- call host immediately
    if (passwordWasCached(host, matchedKeys)) {
      return GetKeyPasswordPmInitResult.CachedPasswordFound;
    }

    initModelProperties(
        matchedKeys.stream().map(MatchedKey::getMatchedKey).collect(Collectors.toList()));
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

    // now check if we have matching key with empty password
    for (MatchedKey k : matchedKeys) {
      try {
        keyFilesOperations.validateDecryptionKeyPassword(
            k.getRequestedKeyId(), k.getMatchedKey(), "");
        // empty password worked
        PasswordDeterminedForKey ret =
            new PasswordDeterminedForKey(k.getRequestedKeyId(), k.getMatchedKey(), "");
        CACHE_KEYID_TO_PASSWORD.put(k.getRequestedKeyId(), ret);
        host.onPasswordDeterminedForKey(ret);
        return true;
      } catch (ValidationException e) {
        // nope, empty password doesn't work.
      }
    }

    return false;
  }

  private void initModelProperties(List<Key> keys) {
    PropertyNameResolver<CreateKeyParams> nameResolver =
        propertyNameResolverFactory.getResolver(CreateKeyParams.class);

    purpose =
        new ModelProperty<>(
            this,
            new ValueAdapterHolderImpl<>(text(initParams.purpose)),
            "purpose",
            validationErrors);
    decryptionKeys =
        new ModelListProperty<>(this, new ValueAdapterReadonlyImpl<>(keys), "decryptionKeys");
    selectedKey =
        new ModelSelInComboBoxProperty<>(
            this, new ValueAdapterHolderImpl<>(keys.get(0)), "selectedKey", decryptionKeys);
    passphrase =
        new ModelProperty<>(
            this,
            new ValueAdapterHolderImpl<>(),
            nameResolver.resolve(CreateKeyParams::getPassphrase),
            validationErrors);
    passphrase.getModelPropertyAccessor().addPropertyChangeListener(onPassphraseChanged);
  }

  private final PropertyChangeListener onPassphraseChanged =
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          validatePassword();
        }

        private void validatePassword() {
          if (StringUtils.hasText(passphrase.getValue())) {
            validationErrors.removeAll(
                ValidationErrorsUtils.findErrorsForField(
                    passphrase.getPropertyName(), validationErrors));
            // NOTE: We also can try to check password while user is
            // typing... Should we do that? It might be annoying to see red
            // border before user completes writing password. And once hes
            // done he just press enter and finf out whether password was
            // correct
          } else {
            validationErrors.add(new MustNotBeNull(passphrase.getPropertyName()));
          }
        }
      };

  protected final Action actionCancel =
      new LocalizedActionEx("action.cancel", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          host.onCancel();
        }
      };

  protected final Action actionChooseKey =
      new LocalizedActionEx("action.ok", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          Key key = selectedKey.getValue();
          String passwordStr = passphrase.getValue();

          Optional<MatchedKey> matchedKey =
              matchedKeys.stream().filter(x -> x.getMatchedKey() == key).findFirst();
          Preconditions.checkState(
              matchedKey.isPresent(), "Failed to find matching key to key selected in combobox");
          String requestedKeyId = matchedKey.get().getRequestedKeyId();

          try {
            validationErrors.removeAll(
                ValidationErrorsUtils.findErrorsForField(
                    passphrase.getPropertyName(), validationErrors));
            if (!StringUtils.hasText(passphrase.getValue())) {
              validationErrors.add(new MustNotBeNull(passphrase.getPropertyName()));
              return;
            }

            keyFilesOperations.validateDecryptionKeyPassword(requestedKeyId, key, passwordStr);
          } catch (ValidationException fve) {
            validationErrors.addAll(fve.getErrors());
            return;
          }

          // If everything is ok -- return
          PasswordDeterminedForKey ret =
              new PasswordDeterminedForKey(requestedKeyId, key, passwordStr);
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

  @Subscribe
  public void onKeyChanged(EntityChangedEvent<Key> e) {
    if (!e.isTypeOf(Key.class) || e.getChangeType() == ChangeType.ADDED) {
      return;
    }

    for (Iterator<Entry<String, PasswordDeterminedForKey>> iter =
            CACHE_KEYID_TO_PASSWORD.entrySet().iterator();
        iter.hasNext(); ) {

      if (e.getValue().getKeyData().isHasAlternativeId(iter.next().getKey())) {
        iter.remove();
        log.debug(
            "Removed cached password for changed key " + e.getValue().getKeyInfo().getKeyId());
      }
    }
  }

  public ModelSelInComboBoxPropertyAccessor<Key> getSelectedKey() {
    return selectedKey.getModelSelInComboBoxPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getPassphrase() {
    return passphrase.getModelPropertyAccessor();
  }

  public List<MatchedKey> getMatchedKeys() {
    return matchedKeys;
  }

  public ModelPropertyAccessor<String> getPurpose() {
    return purpose.getModelPropertyAccessor();
  }
}
