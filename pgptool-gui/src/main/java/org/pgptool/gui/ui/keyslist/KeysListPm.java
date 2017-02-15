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

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.FolderChooserDialog;
import org.pgptool.gui.ui.tools.browsefs.SaveFileChooserDialog;
import org.pgptool.gui.ui.tools.browsefs.ValueAdapterPersistentPropertyImpl;
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
	private static Logger log = Logger.getLogger(KeysListPm.class);

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

	private Comparator<Key<KeyData>> keySorterByNameAsc = new ComparatorKeyByNameImpl<KeyData>();

	public void init(KeysListHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		List<Key<KeyData>> initialKeys = keyRingService.readKeys();
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
				File suggestedFileName = suggestFileNameForKey(key, ofd.getCurrentDirectory().getAbsolutePath());
				ofd.setSelectedFile(suggestedFileName);
			}

		};
	}

	private File suggestFileNameForKey(Key<KeyData> key, String basePathNoSlash) {
		String userName = key.getKeyInfo().buildUserNameOnly();
		File suggestedFileName = new File(basePathNoSlash + File.separator + userName + ".asc");
		return suggestedFileName;
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
				browseForFolder(FilenameUtils.getFullPath(targetFile));
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
				UiUtils.messageBox(findRegisteredWindowIfAny(), text("keys.privateKey.exportWarning"),
						text("term.attention"), JOptionPane.WARNING_MESSAGE);
				browseForFolder(FilenameUtils.getFullPath(targetFile));
			}
		}
	};

	@SuppressWarnings("serial")
	public Action actionExportAllPublicKeys = new LocalizedAction("keys.exportAllPublic") {
		private FolderChooserDialog folderChooserDialog;

		@Override
		public void actionPerformed(ActionEvent e) {
			ArrayList<Key<KeyData>> keys = new ArrayList<>(tableModelProp.getList());
			Preconditions.checkState(keys.size() > 0,
					"Export all public keys action was triggered while there is no keys to export");

			String newFolder = getFolderChooserDialog().askUserForFolder(findRegisteredWindowIfAny());
			if (newFolder == null) {
				return;
			}

			int keysExported = 0;
			int keysTotal = keys.size();
			try {
				File folder = new File(newFolder);
				Preconditions.checkArgument(folder.exists() || folder.mkdirs(),
						"Failed to verify target folder existance " + newFolder);
				for (int i = 0; i < keys.size(); i++) {
					Key<KeyData> key = keys.get(i);
					File targetFile = suggestFileNameForKey(key, newFolder);
					keyFilesOperations.exportPublicKey(key, targetFile.getAbsolutePath());
					keysExported++;
				}
			} catch (Throwable t) {
				log.error("Failed to export keys", t);
				EntryPoint.reportExceptionToUser("error.failedToExportKeys", t, keysExported, keysTotal);
			} finally {
				if (keysExported > 0) {
					browseForFolder(newFolder);
				}
			}
		}

		public FolderChooserDialog getFolderChooserDialog() {
			if (folderChooserDialog == null) {
				ValueAdapterPersistentPropertyImpl<String> exportedKeysLocation = new ValueAdapterPersistentPropertyImpl<String>(
						configPairs, "KeysListPm.exportedKeysLocation", null);
				folderChooserDialog = new FolderChooserDialog(text("keys.chooseFolderForKeysExport"),
						exportedKeysLocation);
			}
			return folderChooserDialog;
		}
	};

	private void browseForFolder(String targetFileName) {
		try {
			Desktop.getDesktop().browse(new File(targetFileName).toURI());
		} catch (Throwable t) {
			log.warn("Failed to open folder for exported key", t);
		}
	}

	private Action[] contextMenuActions = new Action[] { actionExportPublicKey, actionExportPrivateKey, null,
			actionDeleteKey };

	private KeysTablePm keysTablePm = new KeysTablePm() {
		@Override
		public Action getActionForRowDoubleClick() {
			return actionActivate;
		}

		@Override
		public ModelTablePropertyAccessor<Key<KeyData>> getKeys() {
			return tableModelProp.getModelTablePropertyAccessor();
		}

		@Override
		public ModelPropertyAccessor<Key<KeyData>> getSelectedRow() {
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

	protected Action getActionCreate() {
		return host.getActionCreateKey();
	}

	public KeysTablePm getKeysTablePm() {
		return keysTablePm;
	}
}
