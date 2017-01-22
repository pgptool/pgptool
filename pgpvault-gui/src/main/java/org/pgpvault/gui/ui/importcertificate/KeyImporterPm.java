package org.pgpvault.gui.ui.importcertificate;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgpvault.gui.app.EntryPoint;
import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
import org.pgpvault.gui.encryption.api.KeyFilesOperations;
import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;
import org.pgpvault.gui.encryption.api.dto.KeyInfo;
import org.pgpvault.gui.tools.PathUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

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
		if ((fileToLoad = askUserForFile()) == null) {
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

	private String askUserForFile() {
		JFileChooser ofd = buildFileChooserDialog();

		int result = ofd.showOpenDialog(findRegisteredWindowIfAny());
		if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File retFile = ofd.getSelectedFile();
		if (retFile == null) {
			return null;
		}

		String ret = retFile.getAbsolutePath();
		configPairs.put(BROWSE_FOLDER, PathUtils.extractBasePath(ret));
		return ret;
	}

	private JFileChooser buildFileChooserDialog() {
		JFileChooser ofd = new JFileChooser();
		ofd.setFileFilter(new FileNameExtensionFilter("ASC Files", "asc"));
		ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
		ofd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		ofd.setAcceptAllFileFilterUsed(true);
		ofd.setMultiSelectionEnabled(false);
		ofd.setDialogTitle(Messages.get("action.importPgpCertificate"));
		ofd.setApproveButtonText(Messages.get("action.choose"));
		suggestInitialDirectory(ofd);
		return ofd;
	}

	private void suggestInitialDirectory(JFileChooser ofd) {
		try {
			String lastChosenDestination = configPairs.find(BROWSE_FOLDER, null);
			String pathname = StringUtils.hasText(lastChosenDestination) ? lastChosenDestination
					: SystemUtils.getUserHome().getAbsolutePath();
			ofd.setCurrentDirectory(new File(pathname));
		} catch (Throwable t) {
			log.warn("Failed to set suggested location", t);
		}
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
				EntryPoint.showMessageBox(null, Messages.get("phrase.keyImportedSuccessfully"),
						Messages.get("term.confirmation"), JOptionPane.INFORMATION_MESSAGE);
				host.handleImporterFinished();
			} catch (Throwable t) {
				log.error("Failed to import", t);
				EntryPoint.reportExceptionToUser("exception.failedToImportPgpCertificate", t);
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
			if ((fileToLoad = askUserForFile()) == null) {
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
