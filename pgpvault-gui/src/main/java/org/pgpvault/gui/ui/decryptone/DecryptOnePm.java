package org.pgpvault.gui.ui.decryptone;

import static org.pgpvault.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgpvault.gui.app.EntryPoint;
import org.pgpvault.gui.app.MessageSeverity;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
import org.pgpvault.gui.decryptedlist.api.DecryptedFile;
import org.pgpvault.gui.decryptedlist.api.DecryptedHistoryService;
import org.pgpvault.gui.encryption.api.EncryptionService;
import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;
import org.pgpvault.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgpvault.gui.tools.PathUtils;
import org.pgpvault.gui.ui.encryptone.EncryptOnePm;
import org.pgpvault.gui.ui.encryptone.EncryptionDialogParameters;
import org.pgpvault.gui.ui.tools.ExistingFileChooserDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.summerb.approaches.security.api.exceptions.InvalidPasswordException;
import org.summerb.approaches.validation.ValidationError;
import org.summerb.approaches.validation.ValidationErrorsUtils;
import org.summerb.approaches.validation.errors.FieldRequiredValidationError;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.lists.ModelListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelSelInComboBoxProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelSelInComboBoxPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class DecryptOnePm extends PresentationModelBase {
	private static final String FN_PASSWORD = "password";
	private static final String FN_SOURCE_FILE = "sourceFile";
	private static final String FN_TARGET_FILE = "targetFile";

	private static Logger log = Logger.getLogger(DecryptOnePm.class);

	private static final String SOURCE_FOLDER = "DecryptOnePm.SOURCE_FOLDER";
	private static final String CONFIG_PAIR_BASE = "Decrypt:";

	private static final Map<String, String> CACHE_KEYID_TO_PASSWORD = new HashMap<>();

	private static final String[] extensions = new String[] { "gpg", "pgp", "asc" };

	@Autowired
	private ConfigPairs configPairs;
	@Autowired
	private DecryptedTempFolder decryptedTempFolder;
	@Autowired
	@Resource(name = "keyRingService")
	private KeyRingService<KeyData> keyRingService;
	@Autowired
	@Resource(name = "encryptionService")
	private EncryptionService<KeyData> encryptionService;
	@Autowired
	private DecryptedHistoryService decryptedHistoryService;

	private DecryptOneHost host;

	private ModelProperty<String> sourceFile;
	private ModelProperty<Boolean> isUseSameFolder;
	private ModelProperty<Boolean> isUseTempFolder;
	private ModelProperty<Boolean> isUseBrowseFolder;
	private ModelProperty<String> targetFile;
	private ModelProperty<Boolean> targetFileEnabled;
	private ModelSelInComboBoxProperty<Key<KeyData>> selectedKey;
	private ModelListProperty<Key<KeyData>> decryptionKeys;
	private ModelProperty<String> password;
	private ModelProperty<Boolean> isDeleteSourceAfter;
	private ModelProperty<Boolean> isOpenTargetFolderAfter;
	private ModelProperty<Boolean> isOpenAssociatedApplication;
	private ListEx<ValidationError> validationErrors = new ListExImpl<ValidationError>();

	private ExistingFileChooserDialog sourceFileChooser;
	private Set<String> sourceFileRecipientsKeysIds;

	public boolean init(DecryptOneHost host, String optionalSource) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		if (!doWeHaveKeysToDecryptWith()) {
			return false;
		}

		initModelProperties();

		if (optionalSource == null) {
			if (getSourceFileChooser().askUserForFile() == null) {
				return false;
			}
		} else {
			sourceFile.setValueByOwner(optionalSource);
		}

		return true;
	}

	private void initModelProperties() {
		sourceFile = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), FN_SOURCE_FILE, validationErrors);
		sourceFile.getModelPropertyAccessor().addPropertyChangeListener(onSourceFileModified);

		isUseTempFolder = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(true), "saveToTempFolder");
		isUseSameFolder = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "saveToSameFolder");
		isUseBrowseFolder = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "saveToBrowseFolder");
		targetFile = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), FN_TARGET_FILE, validationErrors);
		targetFile.getModelPropertyAccessor().addPropertyChangeListener(onTargetFileModified);
		targetFileEnabled = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "targetFileEnabled");
		isUseBrowseFolder.getModelPropertyAccessor().addPropertyChangeListener(onUseBrowseFolderChanged);
		onUseBrowseFolderChanged.propertyChange(null);

		decryptionKeys = new ModelListProperty<Key<KeyData>>(this,
				new ValueAdapterReadonlyImpl<List<Key<KeyData>>>(keyRingService.readKeys()), "decryptionKeys");
		selectedKey = new ModelSelInComboBoxProperty<Key<KeyData>>(this, new ValueAdapterHolderImpl<Key<KeyData>>(null),
				"projects", decryptionKeys);
		selectedKey.getModelPropertyAccessor().addPropertyChangeListener(onSelectedKeyChanged);
		password = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), FN_PASSWORD, validationErrors);
		password.getModelPropertyAccessor().addPropertyChangeListener(onPasswordChanged);

		isDeleteSourceAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "deleteSourceAfter");
		isOpenTargetFolderAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "openTargetFolder");
		isOpenAssociatedApplication = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false),
				"openAssociatedApplication");
	}

	private PropertyChangeListener onSelectedKeyChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			trySuggestPasswordBasedOnKey();
			updatePrimaryOperationAvailability();
		}

		private void trySuggestPasswordBasedOnKey() {
			if (selectedKey.hasValue()) {
				String passwordUsedBefore = CACHE_KEYID_TO_PASSWORD.get(selectedKey.getValue().getKeyInfo().getKeyId());
				if (passwordUsedBefore != null) {
					password.setValueByOwner(passwordUsedBefore);
				}
			}
		}
	};

	private PropertyChangeListener onPasswordChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			validatePassword();
			updatePrimaryOperationAvailability();
		}

		private void validatePassword() {
			if (StringUtils.hasText(password.getValue())) {
				validationErrors.removeAll(ValidationErrorsUtils.findErrorsForField(FN_PASSWORD, validationErrors));
				// NOTE: We also can try to check password while user is
				// typing... Should we do that? It might be annoying to see red
				// border before user completes writing password. And once hes
				// done he just press enter and finf out whether password was
				// correct
			} else {
				validationErrors.add(new FieldRequiredValidationError(FN_PASSWORD));
			}
		}
	};

	private String askUserForTargetFile() {
		try {
			JFileChooser ofd = new JFileChooser();
			ofd.setFileSelectionMode(JFileChooser.FILES_ONLY);
			ofd.setMultiSelectionEnabled(false);
			ofd.setDialogTitle(text("action.chooseTargetFile"));
			ofd.setApproveButtonText(text("action.choose"));
			suggestTargetFileForFileChooser(ofd);
			ofd.setAcceptAllFileFilterUsed(true);

			int result = ofd.showSaveDialog(findRegisteredWindowIfAny());
			if (result != JFileChooser.APPROVE_OPTION) {
				return null;
			}
			File retFile = ofd.getSelectedFile();
			if (retFile == null) {
				return null;
			}

			String ret = retFile.getAbsolutePath();

			targetFile.setValueByOwner(ret);
			return ret;
		} finally {
			validateTargetFile();
		}
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

	private boolean doWeHaveKeysToDecryptWith() {
		if (!keyRingService.readKeys().isEmpty()) {
			return true;
		}
		EntryPoint.showMessageBox(text("phrase.noKeysForDecryption"), text("term.attention"), MessageSeverity.WARNING);
		host.getActionToOpenCertificatesList().actionPerformed(null);
		if (keyRingService.readKeys().isEmpty()) {
			return false;
		}
		return true;
	}

	public ExistingFileChooserDialog getSourceFileChooser() {
		if (sourceFileChooser == null) {
			sourceFileChooser = new ExistingFileChooserDialog(findRegisteredWindowIfAny(), configPairs, SOURCE_FOLDER) {
				@Override
				protected void doFileChooserPostConstruct(JFileChooser ofd) {
					super.doFileChooserPostConstruct(ofd);

					ofd.setAcceptAllFileFilterUsed(false);
					ofd.addChoosableFileFilter(
							new FileNameExtensionFilter("Encrypted files (.gpg, .pgp, .asc)", extensions));
					ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
					ofd.setFileFilter(ofd.getChoosableFileFilters()[0]);

					ofd.setDialogTitle(text("phrase.selectFileToDecrypt"));
				}

				@Override
				protected String handleFileWasChosen(String filePathName) {
					if (filePathName == null) {
						return null;
					}
					sourceFile.setValueByOwner(filePathName);
					return filePathName;
				}
			};
		}
		return sourceFileChooser;
	}

	private PropertyChangeListener onTargetFileModified = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			clearValidationErrorsFromTargetFile();
			validateTargetFile();
		}
	};

	private boolean validateTargetFile() {
		if (!isUseBrowseFolder.getValue()) {
			return true;
		}
		if (!StringUtils.hasText(targetFile.getValue())) {
			validationErrors.add(new FieldRequiredValidationError(FN_TARGET_FILE));
			return false;
		}
		return true;
	}

	private void clearValidationErrorsFromTargetFile() {
		validationErrors.removeAll(ValidationErrorsUtils.findErrorsForField(FN_TARGET_FILE, validationErrors));
	}

	private PropertyChangeListener onSourceFileModified = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			log.debug("Source changed to : " + sourceFile.getValue());

			if (!updateDecryptionKeysList()) {
				updatePrimaryOperationAvailability();
				return;
			}

			updatePrimaryOperationAvailability();

			DecryptionDialogParameters params = findParamsBasedOnSourceFile(sourceFile.getValue());
			if (params != null) {
				useSugestedParameters(params);
			}
		}

		@SuppressWarnings("deprecation")
		private boolean updateDecryptionKeysList() {
			try {
				validationErrors.removeAll(ValidationErrorsUtils.findErrorsForField(FN_SOURCE_FILE, validationErrors));

				decryptionKeys.getList().clear();
				selectedKey.setValueByOwner(null);
				String sourceFileStr = sourceFile.getValue();
				if (!StringUtils.hasText(sourceFileStr)) {
					validationErrors.add(new FieldRequiredValidationError(FN_SOURCE_FILE));
					return false;
				}

				if (!new File(sourceFileStr).exists()) {
					validationErrors.add(new ValidationError("error.thisFileDoesntExist", FN_SOURCE_FILE));
					return false;
				}

				sourceFileRecipientsKeysIds = encryptionService.findKeyIdsForDecryption(sourceFileStr);
				List<Key<KeyData>> keys = keyRingService.findMatchingKeysByAlternativeIds(sourceFileRecipientsKeysIds);
				decryptionKeys.getList().addAll(keys);

				if (decryptionKeys.getList().size() > 0) {
					preselectKey();
				} else {
					validationErrors.add(new ValidationError("error.noMatchingKeysRegistered", FN_SOURCE_FILE));
					return false;
				}

				return true;
			} catch (Throwable t) {
				log.error("Failed to find decryption keys", t);
				validationErrors.add(
						new ValidationError("error.failedToDetermineDecryptionMetodsForGivenFile", FN_SOURCE_FILE));
				return false;
			}
		}

		private void preselectKey() {
			for (Key<KeyData> k : decryptionKeys.getList()) {
				if (CACHE_KEYID_TO_PASSWORD.containsKey(k.getKeyInfo().getKeyId())) {
					selectedKey.setValueByOwner(k);
					break;
				}
			}
			if (!selectedKey.hasValue()) {
				selectedKey.setValueByOwner(decryptionKeys.getList().get(0));
			}
		}

		protected DecryptionDialogParameters findParamsBasedOnSourceFile(String sourceFile) {
			DecryptionDialogParameters params = configPairs.find(CONFIG_PAIR_BASE + sourceFile, null);
			if (params == null) {
				params = configPairs.find(CONFIG_PAIR_BASE + PathUtils.extractBasePath(sourceFile), null);
			}
			return params;
		}

		private void useSugestedParameters(DecryptionDialogParameters params) {
			if (params.isUseSameFolder() || params.isUseTempFolder()) {
				targetFile.setValueByOwner("");
			} else {
				if (params.getSourceFile().equals(sourceFile.getValue())) {
					// if suggested parameters are exactly for this file
					targetFile.setValueByOwner(params.getTargetFile());
				} else {
					// case when suggested parameters are provided for neighbor
					targetFile.setValueByOwner(madeUpTargetFileName(sourceFile.getValue(),
							PathUtils.extractBasePath(params.getTargetFile())));
				}
			}
			// NOTE: MAGIC: We need to set it AFTER we set targetFolder. Because
			// then isUseSameFolder onChange handler will not pen folder
			// selection
			// dialog
			if (params.isUseSameFolder()) {
				isUseSameFolder.setValueByOwner(true);
			} else if (params.isUseTempFolder()) {
				isUseTempFolder.setValueByOwner(true);
			} else {
				isUseBrowseFolder.setValueByOwner(true);
			}

			isDeleteSourceAfter.setValueByOwner(params.isDeleteSourceFile());
			isOpenTargetFolderAfter.setValueByOwner(params.isOpenTargetFolder());
			isOpenAssociatedApplication.setValueByOwner(params.isOpenAssociatedApplication());

			Optional<Key<KeyData>> key = decryptionKeys.getList().stream()
					.filter(x -> params.getDecryptionKeyId().equals(x.getKeyInfo().getKeyId())).findFirst();
			if (key.isPresent()) {
				selectedKey.setValueByOwner(key.get());
			}
		}
	};

	private PropertyChangeListener onUseBrowseFolderChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			boolean result = isUseBrowseFolder.getValue();
			actionBrowseTarget.setEnabled(result);
			targetFileEnabled.setValueByOwner(result);
			// NOTE: MAGIC: This event might be triggered when using suggested
			// parameters. So if value is already provided for target file then
			// we'll not show file chooser
			if (result && !StringUtils.hasText(targetFile.getValue())) {
				askUserForTargetFile();
			}

			if (!result) {
				clearValidationErrorsFromTargetFile();
			}
		}
	};

	protected void updatePrimaryOperationAvailability() {
		boolean result = true;
		result &= StringUtils.hasText(sourceFile.getValue()) && new File(sourceFile.getValue()).exists();
		result &= selectedKey.hasValue();
		result &= StringUtils.hasText(password.getValue());
		actionDoOperation.setEnabled(result);
	}

	@SuppressWarnings("serial")
	protected final Action actionDoOperation = new LocalizedAction("action.decrypt") {
		@SuppressWarnings("deprecation")
		@Override
		public void actionPerformed(ActionEvent e) {
			String targetFileName = getEffectiveTargetFileName();
			if (targetFileName == null) {
				return;
			}

			Key<KeyData> key = selectedKey.getValue();
			String sourceFileStr = sourceFile.getValue();

			try {
				encryptionService.decrypt(sourceFileStr, targetFileName, key, password.getValue());
			} catch (InvalidPasswordException ipe) {
				validationErrors.add(new ValidationError(ipe.getMessageCode(), FN_PASSWORD));
				return;
			} catch (Throwable t) {
				log.error("Failed to decrypt", t);
				EntryPoint.reportExceptionToUser("error.failedToDecryptFile", t);
				return;
			}

			// Cache password
			CACHE_KEYID_TO_PASSWORD.put(key.getKeyInfo().getKeyId(), password.getValue());
			// Remember parameters
			persistDecryptionDialogParametersForCurrentInputs();
			persistEncryptionDialogParameters(targetFileName);
			decryptedHistoryService.add(new DecryptedFile(sourceFileStr, targetFileName));

			// Delete source if asked
			if (isDeleteSourceAfter.getValue()) {
				try {
					Preconditions.checkState(new File(sourceFileStr).delete(), "File.delete() returned false");
				} catch (Throwable t) {
					EntryPoint.reportExceptionToUser("error.decryptOkButCantDeleteSource", t);
				}
			}

			// Open target folder
			boolean confirmationMessageRequired = true;
			if (isOpenTargetFolderAfter.getValue()) {
				browseForFolder(targetFileName);
				confirmationMessageRequired = false;
			}

			// Open target application
			if (isOpenAssociatedApplication.getValue()) {
				openAssociatedApp(targetFileName);
				confirmationMessageRequired = false;
			}

			if (confirmationMessageRequired) {
				EntryPoint.showMessageBox(text("phrase.decryptionSuccess", targetFileName), text("term.success"),
						MessageSeverity.INFO);
			}

			// close window
			host.handleClose();
		}

		private void openAssociatedApp(String targetFileName) {
			try {
				Desktop.getDesktop().open(new File(targetFileName));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("error.decryptOkButCantBrowseForFolder", t);
			}
		}

		private void browseForFolder(String targetFileName) {
			try {
				Desktop.getDesktop().browse(new File(targetFileName).getParentFile().toURI());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("error.decryptOkButCantBrowseForFolder", t);
			}
		}

		private String getEffectiveTargetFileName() {
			if (isUseSameFolder.getValue()) {
				return madeUpTargetFileName(sourceFile.getValue(), PathUtils.extractBasePath(sourceFile.getValue()));
			} else if (isUseTempFolder.getValue()) {
				return madeUpTargetFileName(sourceFile.getValue(), decryptedTempFolder.getTempFolderBasePath());
			}

			// Validation for target folder!! ---
			String targetFileName = targetFile.getValue();
			if (!validateTargetFile()) {
				return null;
			}

			File parentFolder = new File(targetFileName).getParentFile();
			Preconditions.checkState(parentFolder.exists() || parentFolder.mkdirs(),
					"Failed to ensure all parents directories created");
			return targetFileName;
		}

		private void persistDecryptionDialogParametersForCurrentInputs() {
			DecryptionDialogParameters dialogParameters = buildDecryptionDialogParameters();
			configPairs.put(CONFIG_PAIR_BASE + dialogParameters.getSourceFile(), dialogParameters);
			configPairs.put(CONFIG_PAIR_BASE + PathUtils.extractBasePath(dialogParameters.getSourceFile()),
					dialogParameters);
		}

		/**
		 * We use this method to store parameters that program will suggest to
		 * use when user will desire to encrypt back file that was just
		 * decrypted
		 * 
		 * NOTE: Slight SRP concern here. We're interfering with responsibility
		 * area of {@link EncryptOnePm}
		 * 
		 * @param decryptedFile
		 *            decrypted file path name
		 */
		protected void persistEncryptionDialogParameters(String decryptedFile) {
			EncryptionDialogParameters dialogParameters = buildEncryptionDialogParameters(decryptedFile);
			configPairs.put(EncryptOnePm.CONFIG_PAIR_BASE + decryptedFile, dialogParameters);
		}

		private EncryptionDialogParameters buildEncryptionDialogParameters(String decryptedFile) {
			EncryptionDialogParameters ret = new EncryptionDialogParameters();
			ret.setPropagatedFromDecrypt(true);
			ret.setSourceFile(decryptedFile);
			ret.setUseSameFolder(false);
			ret.setTargetFile(sourceFile.getValue());
			ret.setDeleteSourceFile(true); // questionable
			ret.setRecipientsKeysIds(new ArrayList<>(sourceFileRecipientsKeysIds));
			return ret;
		}

		private DecryptionDialogParameters buildDecryptionDialogParameters() {
			DecryptionDialogParameters ret = new DecryptionDialogParameters();
			ret.setSourceFile(sourceFile.getValue());
			ret.setUseSameFolder(isUseSameFolder.getValue());
			ret.setUseTempFolder(isUseTempFolder.getValue());
			ret.setTargetFile(targetFile.getValue());
			ret.setDecryptionKeyId(selectedKey.getValue().getKeyInfo().getKeyId());
			ret.setDeleteSourceFile(isDeleteSourceAfter.getValue());
			ret.setOpenTargetFolder(isOpenTargetFolderAfter.getValue());
			ret.setOpenAssociatedApplication(isOpenAssociatedApplication.getValue());
			return ret;
		}
	};

	private String madeUpTargetFileName(String sourceFileName, String targetBasedPath) {
		return targetBasedPath + File.separator + FilenameUtils.getBaseName(sourceFileName);
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
			getSourceFileChooser().askUserForFile();
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

	public ModelPropertyAccessor<Boolean> getIsUseTempFolder() {
		return isUseTempFolder.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getIsUseBrowseFolder() {
		return isUseBrowseFolder.getModelPropertyAccessor();
	}

	public ModelSelInComboBoxPropertyAccessor<Key<KeyData>> getSelectedKey() {
		return selectedKey.getModelSelInComboBoxPropertyAccessor();
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

	public ModelPropertyAccessor<Boolean> getIsOpenAssociatedApplication() {
		return isOpenAssociatedApplication.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getPassword() {
		return password.getModelPropertyAccessor();
	}

	public static boolean isItLooksLikeYourSourceFile(String file) {
		return new File(file).exists()
				&& containsIgnoreCase(extensions, FilenameUtils.getExtension(file).toLowerCase());
	}

	private static boolean containsIgnoreCase(String[] arr, String subject) {
		for (String s : arr) {
			if (s.equalsIgnoreCase(subject)) {
				return true;
			}
		}
		return false;
	}

}
