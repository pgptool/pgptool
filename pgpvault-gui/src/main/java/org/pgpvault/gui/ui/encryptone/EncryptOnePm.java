package org.pgpvault.gui.ui.encryptone;

import static org.pgpvault.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgpvault.gui.app.EntryPoint;
import org.pgpvault.gui.app.MessageSeverity;
import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
import org.pgpvault.gui.encryption.api.EncryptionService;
import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;
import org.pgpvault.gui.tools.PathUtils;
import org.pgpvault.gui.ui.decryptone.DecryptOnePm;
import org.pgpvault.gui.ui.tools.ExistingFileChooserDialog;
import org.pgpvault.gui.ui.tools.ListChangeListenerAnyEventImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.lists.ModelListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultSelInListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultSelInListPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class EncryptOnePm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(EncryptOnePm.class);

	private static final String ENCRYPTED_FILE_EXTENSION = "pgp";
	private static final String SOURCE_FOLDER = "EncryptOnePm.SOURCE_FOLDER";
	public static final String CONFIG_PAIR_BASE = "Encrypt:";

	@Autowired
	private ConfigPairs configPairs;

	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	@Autowired
	@Resource(name = "encryptionService")
	private EncryptionService<KeyData> encryptionService;

	private EncryptOneHost host;

	private ModelProperty<String> sourceFile;
	private ModelProperty<Boolean> isUseSameFolder;
	private ModelProperty<String> targetFile;
	private ModelProperty<Boolean> targetFileEnabled;
	private ModelMultSelInListProperty<Key<KeyData>> selectedRecipients;
	private ModelListProperty<Key<KeyData>> availabileRecipients;
	private ModelProperty<Boolean> isDeleteSourceAfter;
	private ModelProperty<Boolean> isOpenTargetFolderAfter;

	private ExistingFileChooserDialog sourceFileChooser;

	public boolean init(EncryptOneHost host, String optionalSource) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		if (!doWeHaveKeysToEncryptWith()) {
			return false;
		}

		initModelProperties();

		if (optionalSource == null) {
			if (askUserForSourceFile() == null) {
				return false;
			}
		} else {
			sourceFile.setValueByOwner(optionalSource);
		}

		return true;
	}

	private String askUserForSourceFile() {
		String selectedSourceFile;
		if ((selectedSourceFile = getSourceFileChooser().askUserForFile()) == null) {
			return null;
		}
		sourceFile.setValueByOwner(selectedSourceFile);
		return selectedSourceFile;
	}

	private String askUserForTargetFile() {
		JFileChooser ofd = new JFileChooser();
		ofd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		ofd.setMultiSelectionEnabled(false);
		ofd.setDialogTitle(Messages.get("action.chooseTargetFile"));
		ofd.setApproveButtonText(Messages.get("action.choose"));
		suggestTargetFileForFileChooser(ofd);

		ofd.setAcceptAllFileFilterUsed(false);
		ofd.addChoosableFileFilter(new FileNameExtensionFilter("GPG Files (.pgp)", "pgp"));
		// NOTE: Should we support other extensions?....
		ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
		ofd.setFileFilter(ofd.getChoosableFileFilters()[0]);

		int result = ofd.showSaveDialog(findRegisteredWindowIfAny());
		if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}
		File retFile = ofd.getSelectedFile();
		if (retFile == null) {
			return null;
		}

		String ret = retFile.getAbsolutePath();
		ret = fixTargetFileExtensionIfNeeded(ofd, ret);

		targetFile.setValueByOwner(ret);
		return ret;
	}

	private String fixTargetFileExtensionIfNeeded(JFileChooser ofd, String filePathName) {
		FileFilter fileExtFilter = ofd.getFileFilter();
		if (fileExtFilter == ofd.getAcceptAllFileFilter()) {
			return filePathName;
		}
		FileNameExtensionFilter fileNameExtensionFilter = (FileNameExtensionFilter) fileExtFilter;
		String ext = fileNameExtensionFilter.getExtensions()[0];
		if (!ext.equalsIgnoreCase(FilenameUtils.getExtension(filePathName))) {
			filePathName += "." + ext;
			// filePathName = FilenameUtils.getFullPath(filePathName) +
			// FilenameUtils.getBaseName(filePathName) + "."
			// + ext;
		}
		return filePathName;
	}

	private void suggestTargetFileForFileChooser(JFileChooser ofd) {
		String sourceFileStr = sourceFile.getValue();
		if (StringUtils.hasText(targetFile.getValue())) {
			ofd.setCurrentDirectory(new File(PathUtils.extractBasePath(targetFile.getValue())));
			ofd.setSelectedFile(new File(targetFile.getValue()));
		} else if (StringUtils.hasText(sourceFileStr) && new File(sourceFileStr).exists()) {
			String basePath = PathUtils.extractBasePath(sourceFileStr);
			ofd.setCurrentDirectory(new File(basePath));
			ofd.setSelectedFile(new File(madeUpTargetFileName(sourceFileStr, basePath)));
		} else {
			// NOTE: Can't think of a right way to react on this case
		}
	}

	private boolean doWeHaveKeysToEncryptWith() {
		if (!keyRingService.readKeys().isEmpty()) {
			return true;
		}
		EntryPoint.showMessageBox(text("phrase.noKeysForEncryption"), text("term.attention"), MessageSeverity.WARNING);
		host.getActionToOpenCertificatesList().actionPerformed(null);
		if (keyRingService.readKeys().isEmpty()) {
			return false;
		}
		return true;
	}

	private void initModelProperties() {
		sourceFile = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "sourceFile");
		sourceFile.getModelPropertyAccessor().addPropertyChangeListener(onSourceFileModified);

		isUseSameFolder = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(true), "saveToSameFolder");
		isUseSameFolder.getModelPropertyAccessor().addPropertyChangeListener(onUseSameFolderChanged);
		targetFile = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "targetFile");
		targetFileEnabled = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "targetFile");
		onUseSameFolderChanged.propertyChange(null);

		availabileRecipients = new ModelListProperty<Key<KeyData>>(this,
				new ValueAdapterReadonlyImpl<List<Key<KeyData>>>(keyRingService.readKeys()), "availabileRecipients");
		selectedRecipients = new ModelMultSelInListProperty<Key<KeyData>>(this,
				new ValueAdapterHolderImpl<List<Key<KeyData>>>(new ArrayList<Key<KeyData>>()), "projects",
				availabileRecipients);
		selectedRecipients.getModelMultSelInListPropertyAccessor().addListExEventListener(onRecipientsSelectionChanged);
		onRecipientsSelectionChanged.onListChanged();

		isDeleteSourceAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "deleteSourceAfter");
		isOpenTargetFolderAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "openTargetFolder");
	}

	public ExistingFileChooserDialog getSourceFileChooser() {
		if (sourceFileChooser == null) {
			sourceFileChooser = new ExistingFileChooserDialog(findRegisteredWindowIfAny(), configPairs, SOURCE_FOLDER) {
				@Override
				protected void doFileChooserPostConstruct(JFileChooser ofd) {
					super.doFileChooserPostConstruct(ofd);
					ofd.setDialogTitle(Messages.get("phrase.selectFileToEncrypt"));

					ofd.setAcceptAllFileFilterUsed(false);
					ofd.addChoosableFileFilter(notEncryptedFiles);
					ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
					ofd.setFileFilter(ofd.getChoosableFileFilters()[0]);
				}

				private FileFilter notEncryptedFiles = new FileFilter() {
					@Override
					public boolean accept(File f) {
						return !DecryptOnePm.isItLooksLikeYourSourceFile(f.getAbsolutePath());
					}

					@Override
					public String getDescription() {
						return text("phrase.allExceptEncrypted");
					}
				};
			};
		}
		return sourceFileChooser;
	}

	private PropertyChangeListener onSourceFileModified = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			log.debug("Source changed to : " + sourceFile.getValue());

			refreshPrimaryOperationAvailability();

			if (!StringUtils.hasText(sourceFile.getValue())) {
				return;
			}

			EncryptionDialogParameters params = findParamsBasedOnSourceFile(sourceFile.getValue());
			if (params != null) {
				useSugestedParameters(params);
			} else {
				selectSelfAsRecipient();
			}
		}

		protected EncryptionDialogParameters findParamsBasedOnSourceFile(String sourceFile) {
			EncryptionDialogParameters params = configPairs.find(CONFIG_PAIR_BASE + sourceFile, null);
			if (params == null) {
				params = configPairs.find(CONFIG_PAIR_BASE + PathUtils.extractBasePath(sourceFile), null);
			}
			return params;
		}

		private void selectSelfAsRecipient() {
			selectedRecipients.getList().clear();
			for (Key<KeyData> key : availabileRecipients.getList()) {
				if (!key.getKeyData().isCanBeUsedForDecryption()) {
					continue;
				}
				selectedRecipients.getList().add(key);
			}
		}

		private void useSugestedParameters(EncryptionDialogParameters params) {
			useSuggestedTargetFile(params);

			// NOTE: MAGIC: We need to set it AFTER we set targetFolder. Because
			// then isUseSameFolder onChange handler will not open folder
			// selection
			// dialog
			isUseSameFolder.setValueByOwner(params.isUseSameFolder());
			isDeleteSourceAfter.setValueByOwner(params.isDeleteSourceFile());
			isOpenTargetFolderAfter.setValueByOwner(params.isOpenTargetFolder());

			List<String> missedKeys = preselectRecipients(params.getRecipientsKeysIds());
			notifyUserOfMissingKeysIfAny(missedKeys);
		}

		private void useSuggestedTargetFile(EncryptionDialogParameters params) {
			if (!params.isUseSameFolder()) {
				if (params.getSourceFile().equals(sourceFile.getValue())) {
					targetFile.setValueByOwner(params.getTargetFile());
				} else {
					// NOTE: Assuming that params fully valid and target file is
					// provided
					targetFile.setValueByOwner(madeUpTargetFileName(sourceFile.getValue(),
							PathUtils.extractBasePath(params.getTargetFile())));
				}
			} else {
				targetFile.setValueByOwner("");
			}
		}

		private List<String> preselectRecipients(ArrayList<String> recipientsKeysIds) {
			selectedRecipients.getList().clear();
			List<String> missedKeys = new ArrayList<>();
			for (String keyId : recipientsKeysIds) {
				Optional<Key<KeyData>> key = availabileRecipients.getList().stream()
						.filter(x -> x.getKeyData().isHasAlternativeId(keyId)).findFirst();
				if (key.isPresent()) {
					selectedRecipients.getList().add(key.get());
				} else {
					missedKeys.add(keyId);
				}
			}
			return missedKeys;
		}

		private void notifyUserOfMissingKeysIfAny(List<String> missedKeys) {
			if (CollectionUtils.isEmpty(missedKeys)) {
				return;
			}

			EntryPoint.showMessageBox(findRegisteredWindowIfAny(),
					text("error.notAllRecipientsAvailable", Arrays.asList(missedKeys)), text("term.attention"),
					JOptionPane.WARNING_MESSAGE);
		}
	};

	private PropertyChangeListener onUseSameFolderChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			boolean result = !isUseSameFolder.getValue();
			actionBrowseTarget.setEnabled(result);
			targetFileEnabled.setValueByOwner(result);
			// NOTE: MAGIC: This event might be triggered when using suggested
			// parameters. So if value is already provided for target file then
			// we'll not show file chooser
			if (result && !StringUtils.hasText(targetFile.getValue())) {
				askUserForTargetFile();
			}
		}
	};

	protected void refreshPrimaryOperationAvailability() {
		boolean result = true;
		result &= !selectedRecipients.getList().isEmpty();
		result &= StringUtils.hasText(sourceFile.getValue()) && new File(sourceFile.getValue()).exists();
		actionDoOperation.setEnabled(result);
	}

	private ListChangeListenerAnyEventImpl<Key<KeyData>> onRecipientsSelectionChanged = new ListChangeListenerAnyEventImpl<Key<KeyData>>() {
		@Override
		public void onListChanged() {
			refreshPrimaryOperationAvailability();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionDoOperation = new LocalizedAction("action.encrypt") {
		@Override
		public void actionPerformed(ActionEvent e) {
			String targetFileName = getEffectiveTargetFileName();

			try {
				encryptionService.encrypt(sourceFile.getValue(), targetFileName, selectedRecipients.getList());
			} catch (Throwable t) {
				log.error("Failed to encrypt", t);
				EntryPoint.reportExceptionToUser("error.failedToEncryptFile", t);
				return;
			}

			// Delete source if asked
			if (isDeleteSourceAfter.getValue()) {
				try {
					Preconditions.checkState(new File(sourceFile.getValue()).delete(), "File.delete() returned false");
				} catch (Throwable t) {
					EntryPoint.reportExceptionToUser("error.encryptOkButCantDeleteSource", t);
				}
			}

			// Open target folder
			if (isOpenTargetFolderAfter.getValue()) {
				askOperSystemToBrowseForFolder(targetFileName);
			} else {
				// Or show confirmation
				EntryPoint.showMessageBox(text("phrase.encryptionSuccess"), text("term.success"), MessageSeverity.INFO);
			}

			// Remember parameters
			persistDialogParametersForCurrentInputs();

			// close window
			host.handleClose();
		}

		private void askOperSystemToBrowseForFolder(String targetFileName) {
			try {
				Desktop.getDesktop().browse(new File(targetFileName).getParentFile().toURI());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("error.encryptOkButCantBrowseForFolder", t);
			}
		}

		private String getEffectiveTargetFileName() {
			if (!StringUtils.hasText(targetFile.getValue()) || isUseSameFolder.getValue()) {
				isUseSameFolder.setValueByOwner(true);
				return madeUpTargetFileName(sourceFile.getValue(), PathUtils.extractBasePath(sourceFile.getValue()));
			}

			String targetFileName = targetFile.getValue();
			File parentFolder = new File(targetFileName).getParentFile();
			Preconditions.checkState(parentFolder.exists() || parentFolder.mkdirs(),
					"Failed to ensure all parents directories created");
			return targetFileName;
		}

		private void persistDialogParametersForCurrentInputs() {
			EncryptionDialogParameters dialogParameters = buildEncryptionDialogParameters();
			configPairs.put(CONFIG_PAIR_BASE + dialogParameters.getSourceFile(), dialogParameters);
			configPairs.put(CONFIG_PAIR_BASE + PathUtils.extractBasePath(dialogParameters.getSourceFile()),
					dialogParameters);
		}

		private EncryptionDialogParameters buildEncryptionDialogParameters() {
			EncryptionDialogParameters ret = new EncryptionDialogParameters();
			ret.setSourceFile(sourceFile.getValue());
			ret.setUseSameFolder(isUseSameFolder.getValue());
			ret.setTargetFile(targetFile.getValue());
			ret.setDeleteSourceFile(isDeleteSourceAfter.getValue());
			ret.setOpenTargetFolder(isOpenTargetFolderAfter.getValue());
			ret.setRecipientsKeysIds(new ArrayList<>(selectedRecipients.getList().size()));
			for (Key<KeyData> key : selectedRecipients.getList()) {
				ret.getRecipientsKeysIds().add(key.getKeyInfo().getKeyId());
			}
			return ret;
		}
	};

	private String madeUpTargetFileName(String sourceFileName, String targetBasedPath) {
		File fileSource = new File(sourceFileName);
		String fileNameOnlyWoPathAndExtension = fileSource.getName();
		return targetBasedPath + File.separator + fileNameOnlyWoPathAndExtension + "." + ENCRYPTED_FILE_EXTENSION;
	}

	@SuppressWarnings("serial")
	protected final Action actionCancel = new LocalizedAction("action.cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleClose();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionBrowseSource = new LocalizedAction("action.browse") {
		@Override
		public void actionPerformed(ActionEvent e) {
			askUserForSourceFile();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionBrowseTarget = new LocalizedAction("action.browse") {
		@Override
		public void actionPerformed(ActionEvent e) {
			askUserForTargetFile();
		}
	};

	public ModelPropertyAccessor<Boolean> getIsDeleteSourceAfter() {
		return isDeleteSourceAfter.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getIsOpenTargetFolderAfter() {
		return isOpenTargetFolderAfter.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getIsUseSameFolder() {
		return isUseSameFolder.getModelPropertyAccessor();
	}

	public ModelMultSelInListPropertyAccessor<Key<KeyData>> getSelectedRecipients() {
		return selectedRecipients.getModelMultSelInListPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getSourceFile() {
		return sourceFile.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getTargetFile() {
		return targetFile.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getTargetFileEnabled() {
		return targetFileEnabled.getModelPropertyAccessor();
	}

	public static boolean isItLooksLikeYourSourceFile(String file) {
		// NOTE: As of know it's fairly simple -- like if it's not for
		// decryption than it's opposite. Later on we might want to revisit this
		// logic.
		return new File(file).exists() && !DecryptOnePm.isItLooksLikeYourSourceFile(file);
	}

}
