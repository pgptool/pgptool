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
package org.pgptool.gui.ui.importkey;

import java.awt.event.ActionEvent;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JFileChooser;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.encryption.api.dto.KeyInfo;
import org.pgptool.gui.ui.tools.ExistingFileChooserDialog;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.base.View;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class KeyImporterPm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(KeyImporterPm.class);
	private static final String BROWSE_FOLDER = "KeyImporterPm.BROWSE_FOLDER";

	@Autowired
	private ConfigPairs configPairs;

	@Autowired
	@Resource(name = "keyFilesOperations")
	private KeyFilesOperations<KeyData> keyFilesOperations;
	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	private KeyImporterHost host;

	private Key<KeyData> key;
	private ExistingFileChooserDialog sourceFileChooser;

	private ModelProperty<String> user;
	private ModelProperty<String> keyId;
	private ModelProperty<String> keyType;
	private ModelProperty<String> keyAlgorithm;
	private ModelProperty<String> createdOn;
	private ModelProperty<String> expiresAt;
	private ModelProperty<Boolean> isKeyLoaded;
	private ModelProperty<String> filePathName;

	public boolean init(KeyImporterHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		initModelProperties();

		String fileToLoad = null;
		if ((fileToLoad = getSourceFileChooser().askUserForFile()) == null) {
			return false;
		}
		if (!loadKey(fileToLoad)) {
			return false;
		}

		return true;
	}

	private void initModelProperties() {
		filePathName = initStringModelProp("filePathName");
		isKeyLoaded = new ModelProperty<>(this, new ValueAdapterHolderImpl<Boolean>(false), "isKeyLoaded");
		user = initStringModelProp("user");
		keyId = initStringModelProp("keyId");
		keyType = initStringModelProp("keyType");
		keyAlgorithm = initStringModelProp("keyAlgorithm");
		createdOn = initStringModelProp("createdOn");
		expiresAt = initStringModelProp("expiresAt");
		actionDoImport.setEnabled(false);
	}

	private ModelProperty<String> initStringModelProp(String fieldName) {
		return new ModelProperty<String>(this, new ValueAdapterHolderImpl<String>("TBD"), fieldName);
	}

	@Override
	public void registerView(View<?> view) {
		super.registerView(view);
		Preconditions.checkState(host != null);
	}

	public ExistingFileChooserDialog getSourceFileChooser() {
		if (sourceFileChooser == null) {
			sourceFileChooser = new ExistingFileChooserDialog(findRegisteredWindowIfAny(), configPairs, BROWSE_FOLDER) {
				@Override
				protected void doFileChooserPostConstruct(JFileChooser ofd) {
					super.doFileChooserPostConstruct(ofd);
					ofd.setDialogTitle(Messages.get("action.importKey"));
				}
			};
		}
		return sourceFileChooser;
	}

	private boolean loadKey(String fileToLoad) {
		try {
			key = keyFilesOperations.readKeyFromFile(fileToLoad);

			filePathName.setValueByOwner(fileToLoad);

			KeyInfo info = key.getKeyInfo();
			user.setValueByOwner(info.getUser());
			keyId.setValueByOwner(info.getKeyId());
			keyType.setValueByOwner(Messages.get("term." + info.getKeyType().toString()));
			keyAlgorithm.setValueByOwner(info.getKeyAlgorithm());
			createdOn.setValueByOwner(info.getCreatedOn().toString());
			expiresAt.setValueByOwner(info.getExpiresAt() == null ? "" : info.getExpiresAt().toString());

			isKeyLoaded.setValueByOwner(true);
			actionDoImport.setEnabled(true);

			return true;
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser("exception.failedToReadKey", t);
			return false;
		}
	}

	@SuppressWarnings("serial")
	private Action actionDoImport = new LocalizedAction("action.import") {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Preconditions.checkState(key != null, "Key is not loaded");
				keyRingService.addKey(key);
				// NOTE: Decided to turn confirmation off. Feels like it just
				// requires redundant action from user
				// EntryPoint.showMessageBox(null,
				// Messages.get("phrase.keyImportedSuccessfully"),
				// Messages.get("term.confirmation"),
				// JOptionPane.INFORMATION_MESSAGE);
				host.handleImporterFinished();
			} catch (Throwable t) {
				log.error("Failed to import", t);
				EntryPoint.reportExceptionToUser("exception.failedToImportPgpKey", t);
				return;
			}
		}
	};

	@SuppressWarnings("serial")
	private Action actionCancel = new LocalizedAction("action.cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleImporterFinished();
		}
	};

	@SuppressWarnings("serial")
	private Action actionBrowse = new LocalizedAction("action.browse") {
		@Override
		public void actionPerformed(ActionEvent e) {
			String fileToLoad = null;
			if ((fileToLoad = getSourceFileChooser().askUserForFile()) == null) {
				return;
			}
			loadKey(fileToLoad);
		}
	};

	protected Action getActionCancel() {
		return actionCancel;
	}

	protected Action getActionBrowse() {
		return actionBrowse;
	}

	protected Action getActionDoImport() {
		return actionDoImport;
	}

	public ModelPropertyAccessor<String> getUser() {
		return user.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getKeyId() {
		return keyId.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getKeyType() {
		return keyType.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getKeyAlgorithm() {
		return keyAlgorithm.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getCreatedOn() {
		return createdOn.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getExpiresAt() {
		return expiresAt.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getFilePathName() {
		return filePathName.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getIsKeyLoaded() {
		return isKeyLoaded.getModelPropertyAccessor();
	}

}
