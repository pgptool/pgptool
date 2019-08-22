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

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;

import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.ModelTableProperty;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class KeysListPm extends PresentationModelBase {
	// private static Logger log = Logger.getLogger(KeysListPm.class);

	@Autowired
	private EventBus eventBus;
	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private KeysExporterUi keysExporterUi;

	private KeysListHost host;

	private ModelTableProperty<Key> tableModelProp;
	private ModelProperty<Boolean> hasData;

	private Comparator<Key> keySorterByNameAsc = new ComparatorKeyByNameImpl();

	public void init(KeysListHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		List<Key> initialKeys = keyRingService.readKeys();
		initialKeys.sort(keySorterByNameAsc);
		tableModelProp = new ModelTableProperty<>(this, initialKeys, "keys", new KeysTableModel());
		hasData = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(!initialKeys.isEmpty()), "hasData");
		tableModelProp.getModelPropertyAccessor().addPropertyChangeListener(onSelectionChangedHandler);
		onSelectionChangedHandler.propertyChange(null);

		actionExportAllPublicKeys.setEnabled(hasData.getValue());

		eventBus.register(this);
	}

	private PropertyChangeListener onSelectionChangedHandler = new PropertyChangeListener() {
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
			actionExportPrivateKey.setEnabled(key != null && key.getKeyData().isCanBeUsedForDecryption());
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
	private Action actionClose = new LocalizedAction("action.close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
		}
	};

	@SuppressWarnings("serial")
	private Action actionActivate = new LocalizedAction("action.activate") {
		@Override
		public void actionPerformed(ActionEvent e) {
			// TBD: Impl
		}
	};

	@SuppressWarnings("serial")
	private Action actionDeleteKey = new LocalizedAction("action.deleteKey") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!tableModelProp.hasValue()) {
				// silently exit -- not going to complain
				return;
			}

			Key key = tableModelProp.getValue();
			if (!UiUtils.confirmRegular("phrase.areYouSureToDeleteKey", new Object[] { key.getKeyInfo().getUser() },
					findRegisteredWindowIfAny())) {
				return;
			}

			keyRingService.removeKey(key);
		}
	};

	@SuppressWarnings("serial")
	private Action actionExportPublicKey = new LocalizedAction("action.exportPublicKey") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!tableModelProp.hasValue()) {
				return;
			}
			Key key = tableModelProp.getValue();
			keysExporterUi.exportPublicKey(key, findRegisteredWindowIfAny());
		}
	};

	@SuppressWarnings("serial")
	private Action actionExportPrivateKey = new LocalizedAction("action.exportPrivateKey") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!tableModelProp.hasValue()) {
				return;
			}
			Key key = tableModelProp.getValue();
			keysExporterUi.exportPrivateKey(key, findRegisteredWindowIfAny());
		}
	};

	@SuppressWarnings("serial")
	public Action actionExportAllPublicKeys = new LocalizedAction("keys.exportAllPublic") {
		@Override
		public void actionPerformed(ActionEvent e) {
			ArrayList<Key> keys = new ArrayList<>(tableModelProp.getList());
			Preconditions.checkState(keys.size() > 0,
					"Export all public keys action was triggered while there is no keys to export");

			keysExporterUi.exportPublicKeys(keys, findRegisteredWindowIfAny());
		}
	};

	private Action[] contextMenuActions = new Action[] { actionExportPublicKey, actionExportPrivateKey, null,
			actionDeleteKey };

	private KeysTablePm keysTablePm = new KeysTablePm() {
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
