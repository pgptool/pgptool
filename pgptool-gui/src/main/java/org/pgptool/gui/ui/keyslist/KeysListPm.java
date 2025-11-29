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
package org.pgptool.gui.ui.keyslist;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.Action;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.tools.ClipboardUtil;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.easycrud.api.dto.EntityChangedEvent;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.ModelTableProperty;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class KeysListPm extends PresentationModelBaseEx<KeysListHost, Void> {
  // private static Logger log = Logger.getLogger(KeysListPm.class);

  @Autowired private EventBus eventBus;
  @Autowired private KeyRingService keyRingService;
  @Autowired private KeysExporterUi keysExporterUi;
  @Autowired private KeyFilesOperations keyFilesOperations;

  private ModelTableProperty<Key> tableModelProp;
  private ModelProperty<Boolean> hasData;

  private Comparator<Key> keySorterByNameAsc = new ComparatorKeyByNameImpl();

  @Override
  public boolean init(ActionEvent originAction, KeysListHost host, Void initParams) {
    super.init(originAction, host, initParams);
    Preconditions.checkArgument(host != null);

    List<Key> initialKeys = keyRingService.readKeys();
    initialKeys.sort(keySorterByNameAsc);
    tableModelProp = new ModelTableProperty<>(this, initialKeys, "keys", new KeysTableModel());
    hasData =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(!initialKeys.isEmpty()), "hasData");
    tableModelProp.getModelPropertyAccessor().addPropertyChangeListener(onSelectionChangedHandler);
    onSelectionChangedHandler.propertyChange(null);

    actionExportAllPublicKeys.setEnabled(hasData.getValue());

    eventBus.register(this);
    return true;
  }

  private PropertyChangeListener onSelectionChangedHandler =
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Key key = tableModelProp.getValue();
          boolean hasSelection = key != null;
          for (int i = 0; i < contextMenuActions.length; i++) {
            Action action = contextMenuActions[i];
            if (action == null) {
              continue;
            }
            ((Action) action).setEnabled(hasSelection);
          }

          boolean isPrivateKey = key != null && key.getKeyData().isCanBeUsedForDecryption();
          actionExportPrivateKey.setEnabled(isPrivateKey);
          actionChangePassphrase.setEnabled(isPrivateKey);
        }
      };

  @Subscribe
  public void onRowChangedEvent(EntityChangedEvent<?> event) {
    if (!event.isTypeOf(Key.class)) {
      return;
    }

    List<Key> newKeysList = keyRingService.readKeys();
    newKeysList.sort(keySorterByNameAsc);
    tableModelProp.getList().clear();
    tableModelProp.getList().addAll(newKeysList);
    hasData.setValueByOwner(!newKeysList.isEmpty());
    actionExportAllPublicKeys.setEnabled(hasData.getValue());

    // NOTE: Selection is not nicely maintained. Each update will clear the
    // current selection if any
  }

  @Override
  public void detach() {
    super.detach();
    eventBus.unregister(this);
  }

  @SuppressWarnings("serial")
  private Action actionClose =
      new LocalizedActionEx("action.close", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          host.handleClose();
        }
      };

  @SuppressWarnings("serial")
  private Action actionActivate =
      new LocalizedActionEx("action.activate", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          // TBD: Impl
        }
      };

  @SuppressWarnings("serial")
  private Action actionDeleteKey =
      new LocalizedActionEx("action.deleteKey", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          if (!tableModelProp.hasValue()) {
            // silently exit -- not going to complain
            return;
          }

          Key key = tableModelProp.getValue();
          if (!UiUtils.confirmRegular(
              e, "phrase.areYouSureToDeleteKey", new Object[] {key.getKeyInfo().getUser()})) {
            return;
          }

          keyRingService.removeKey(key);
        }
      };

  @SuppressWarnings("serial")
  private Action actionExportPublicKey =
      new LocalizedActionEx("action.exportPublicKey", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          if (!tableModelProp.hasValue()) {
            return;
          }
          Key key = tableModelProp.getValue();
          keysExporterUi.exportPublicKey(key, e);
        }
      };

  @SuppressWarnings("serial")
  private Action actionExportPublicKeyToClipboard =
      new LocalizedActionEx("action.exportPublicKeyToClipboard", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          if (!tableModelProp.hasValue()) {
            return;
          }
          Key key = tableModelProp.getValue();
          String keyAsc = keyFilesOperations.getPublicKeyArmoredRepresentation(key);
          ClipboardUtil.setClipboardText(keyAsc);
        }
      };

  @SuppressWarnings("serial")
  private Action actionExportPrivateKey =
      new LocalizedActionEx("action.exportPrivateKey", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          if (!tableModelProp.hasValue()) {
            return;
          }
          Key key = tableModelProp.getValue();
          keysExporterUi.exportPrivateKey(key, e);
        }
      };

  @SuppressWarnings("serial")
  private Action actionChangePassphrase =
      new LocalizedActionEx("action.changePassphrase", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          if (!tableModelProp.hasValue()) {
            return;
          }
          Key key = tableModelProp.getValue();
          host.changeKeyPassphrase(key, e);
        }
      };

  @SuppressWarnings("serial")
  public Action actionExportAllPublicKeys =
      new LocalizedActionEx("keys.exportAllPublic", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          ArrayList<Key> keys = new ArrayList<>(tableModelProp.getList());
          Preconditions.checkState(
              keys.size() > 0,
              "Export all public keys action was triggered while there is no keys to export");

          keysExporterUi.exportPublicKeys(keys, e);
        }
      };

  private Action[] contextMenuActions =
      new Action[] {
        actionExportPublicKey,
        actionExportPrivateKey,
        actionChangePassphrase,
        null,
        actionExportPublicKeyToClipboard,
        null,
        actionDeleteKey
      };

  private KeysTablePm keysTablePm =
      new KeysTablePm() {
        @Override
        public Action getActionForRowDoubleClick() {
          return actionActivate;
        }

        @Override
        public ModelTablePropertyAccessor<Key> getKeys() {
          return tableModelProp.getModelTablePropertyAccessor();
        }

        @Override
        public ModelPropertyAccessor<Key> getSelectedRow() {
          return tableModelProp.getModelPropertyAccessor();
        }

        @Override
        public ModelPropertyAccessor<Boolean> getHasData() {
          return hasData.getModelPropertyAccessor();
        }

        @Override
        public Action getActionDelete() {
          return actionDeleteKey;
        }

        @Override
        public Action[] getContextMenuActions() {
          return contextMenuActions;
        }
      };

  protected Action getActionClose() {
    return actionClose;
  }

  protected Action getActionImport() {
    return host.getActionImportKey();
  }

  public Action getActionImportFromText() {
    return host.getActionImportKeyFromText();
  }

  protected Action getActionCreate() {
    return host.getActionCreateKey();
  }

  public KeysTablePm getKeysTablePm() {
    return keysTablePm;
  }
}
