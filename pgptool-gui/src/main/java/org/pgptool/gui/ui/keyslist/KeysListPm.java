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
package org.pgptool.gui.ui.keyslist;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.tools.SaveFileChooserDialog;
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
	@Autowired
	private EventBus eventBus;
	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	@Autowired
	private ConfigPairs configPairs;
	@Autowired
	@Resource(name = "keyFilesOperations")
	private KeyFilesOperations<KeyData> keyFilesOperations;

	private KeysListHost host;

	private ModelTableProperty<Key<KeyData>> tableModelProp;
	private ModelProperty<Boolean> hasData;

	public void init(KeysListHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		List<Key<KeyData>> initialKeys = keyRingService.readKeys();
		tableModelProp = new ModelTableProperty<>(this, initialKeys, "keys", new KeysTableModel());
		hasData = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(!initialKeys.isEmpty()), "hasData");
		tableModelProp.getModelPropertyAccessor().addPropertyChangeListener(onSelectionChangedHandler);
		onSelectionChangedHandler.propertyChange(null);

		eventBus.register(this);
	}

	private PropertyChangeListener onSelectionChangedHandler = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Key<KeyData> key = tableModelProp.getValue();
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

		List<Key<KeyData>> newKeysList = keyRingService.readKeys();
		tableModelProp.getList().clear();
		tableModelProp.getList().addAll(newKeysList);
		hasData.setValueByOwner(!newKeysList.isEmpty());

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
			// TODO: Impl
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

			Key<KeyData> key = tableModelProp.getValue();
			if (!UiUtils.confirm("phrase.areYouSureToDeleteKey", new Object[] { key.getKeyInfo().getUser() },
					findRegisteredWindowIfAny())) {
				return;
			}

			keyRingService.removeKey(key);
		}
	};

	public SaveFileChooserDialog buildPrivateKeyTargetChooser(final Key<KeyData> key) {
		return new SaveFileChooserDialog(findRegisteredWindowIfAny(), "action.exportPrivateKey", "action.export",
				configPairs, "ExportKeyDialog") {
			@Override
			protected void onFileChooserPostConstrct(JFileChooser ofd) {
				ofd.setAcceptAllFileFilterUsed(false);
				ofd.addChoosableFileFilter(new FileNameExtensionFilter("Key file armored (.asc)", "asc"));
				ofd.addChoosableFileFilter(new FileNameExtensionFilter("Key file (.bpg)", "bpg"));
				ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
				ofd.setFileFilter(ofd.getChoosableFileFilters()[0]);
			}

			@Override
			protected void suggestTarget(JFileChooser ofd) {
				super.suggestTarget(ofd);

				String userName = key.getKeyInfo().buildUserNameOnly();
				File suggestedFileName = new File(
						ofd.getCurrentDirectory().getAbsolutePath() + File.separator + userName + ".asc");
				ofd.setSelectedFile(suggestedFileName);
			}
		};
	}

	public SaveFileChooserDialog buildPublicKeyTargetChooser(Key<KeyData> key) {
		return new SaveFileChooserDialog(findRegisteredWindowIfAny(), "action.exportPublicKey", "action.export",
				configPairs, "ExportKeyDialog") {
			@Override
			protected void onFileChooserPostConstrct(JFileChooser ofd) {
				ofd.setAcceptAllFileFilterUsed(false);
				ofd.addChoosableFileFilter(new FileNameExtensionFilter("Key file armored (.asc)", "asc"));
				ofd.addChoosableFileFilter(new FileNameExtensionFilter("Key file (.bpg)", "bpg"));
				ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
				ofd.setFileFilter(ofd.getChoosableFileFilters()[0]);
			}

			@Override
			protected void suggestTarget(JFileChooser ofd) {
				super.suggestTarget(ofd);
				String userName = key.getKeyInfo().buildUserNameOnly();
				File suggestedFileName = new File(
						ofd.getCurrentDirectory().getAbsolutePath() + File.separator + userName + ".asc");
				ofd.setSelectedFile(suggestedFileName);
			}
		};
	}

	@SuppressWarnings("serial")
	private Action actionExportPublicKey = new LocalizedAction("action.exportPublicKey") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!tableModelProp.hasValue()) {
				return;
			}
			Key<KeyData> key = tableModelProp.getValue();
			String targetFile = buildPublicKeyTargetChooser(key).askUserForFile();
			if (targetFile != null) {
				keyFilesOperations.exportPublicKey(key, targetFile);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action actionExportPrivateKey = new LocalizedAction("action.exportPrivateKey") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!tableModelProp.hasValue()) {
				return;
			}
			Key<KeyData> key = tableModelProp.getValue();
			String targetFile = buildPrivateKeyTargetChooser(key).askUserForFile();
			if (targetFile != null) {
				keyFilesOperations.exportPrivateKey(key, targetFile);
			}
		}
	};

	private Action[] contextMenuActions = new Action[] { actionExportPublicKey, actionExportPrivateKey, null,
			actionDeleteKey };

	protected Action getActionClose() {
		return actionClose;
	}

	protected Action getActionImport() {
		return host.getActionImportKey();
	}

	protected Action getActionCreate() {
		return host.getActionCreateKey();
	}

	public Action getActionForRowDoubleClick() {
		return actionActivate;
	}

	public ModelTablePropertyAccessor<Key<KeyData>> getTableModelProp() {
		return tableModelProp.getModelTablePropertyAccessor();
	}

	public ModelPropertyAccessor<Key<KeyData>> getSelectedRow() {
		return tableModelProp.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getHasData() {
		return hasData.getModelPropertyAccessor();
	}

	public Action getActionDelete() {
		return actionDeleteKey;
	}

	public Action[] getContextMenuActions() {
		return contextMenuActions;
	}
}
