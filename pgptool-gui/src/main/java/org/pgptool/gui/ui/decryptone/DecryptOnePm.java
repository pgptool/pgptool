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
package org.pgptool.gui.ui.decryptone;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Message;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.bkgoperation.UserRequestedCancellationException;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;
import org.pgptool.gui.decryptedlist.api.MonitoringDecryptedFilesService;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.implpgp.SymmetricEncryptionIsNotSupportedException;
import org.pgptool.gui.encryptionparams.api.EncryptionParamsStorage;
import org.pgptool.gui.filecomparison.ChecksumCalcOutputStreamSupervisor;
import org.pgptool.gui.filecomparison.ChecksumCalcOutputStreamSupervisorImpl;
import org.pgptool.gui.filecomparison.ChecksumCalculationTask;
import org.pgptool.gui.filecomparison.Fingerprint;
import org.pgptool.gui.filecomparison.MessageDigestFactory;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgptool.gui.tools.FileUtilsEx;
import org.pgptool.gui.ui.decryptonedialog.KeyAndPasswordCallback;
import org.pgptool.gui.ui.encryptone.EncryptOnePm;
import org.pgptool.gui.ui.encryptone.EncryptionDialogParameters;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.pgptool.gui.ui.tools.ProgressHandlerPmMixinImpl;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ExistingFileChooserDialog;
import org.pgptool.gui.ui.tools.browsefs.SaveFileChooserDialog;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationErrorsUtils;
import org.summerb.validation.errors.FieldRequiredValidationError;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class DecryptOnePm extends PresentationModelBaseEx<DecryptOneHost, String> {
	private static final String FN_SOURCE_FILE = "sourceFile";
	private static final String FN_TARGET_FILE = "targetFile";

	private static Logger log = Logger.getLogger(DecryptOnePm.class);

	private static final String SOURCE_FOLDER = "DecryptOnePm.SOURCE_FOLDER";
	public static final String CONFIG_PAIR_BASE = "Decrypt:";

	private static final String[] EXTENSIONS = new String[] { "gpg", "pgp", "asc" };

	@Autowired
	private ConfigPairs appProps;
	@Autowired
	private ConfigPairs decryptionParams;
	@Autowired
	private ExecutorService executorService;
	@Autowired
	private EncryptionParamsStorage encryptionParamsStorage;
	@Autowired
	private DecryptedTempFolder decryptedTempFolder;
	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private EncryptionService encryptionService;
	@Autowired
	private MonitoringDecryptedFilesService monitoringDecryptedFilesService;
	@Autowired
	private MessageDigestFactory messageDigestFactory;

	private ModelProperty<String> sourceFile;
	private ModelProperty<Boolean> isUseSameFolder;
	private ModelProperty<Boolean> isUseTempFolder;
	private ModelProperty<Boolean> isUseBrowseFolder;
	private ModelProperty<String> targetFile;
	private ModelProperty<Boolean> targetFileEnabled;
	private ModelProperty<Boolean> isDeleteSourceAfter;
	private ModelProperty<Boolean> isOpenTargetFolderAfter;
	private ModelProperty<Boolean> isOpenAssociatedApplication;
	private ListEx<ValidationError> validationErrors = new ListExImpl<ValidationError>();

	private ExistingFileChooserDialog sourceFileChooser;
	private SaveFileChooserDialog targetFileChooser;

	private Set<String> sourceFileRecipientsKeysIds;
	private PasswordDeterminedForKey keyAndPassword;
	private String anticipatedTargetFileName;
	private DecryptionDialogParameters decryptionDialogParameters;

	private ModelProperty<Boolean> isProgressVisible;
	private ModelProperty<Integer> progressValue;
	private ModelProperty<String> progressNote;
	private ModelProperty<Boolean> isDisableControls;
	private ProgressHandlerPmMixinImpl progressHandler;
	private Thread operationThread;

	@Override
	public boolean init(ActionEvent originAction, DecryptOneHost host, String optionalSource) {
		super.init(originAction, host, optionalSource);
		Preconditions.checkArgument(host != null);

		if (!doWeHaveKeysToDecryptWith()) {
			return false;
		}

		initModelProperties();

		if (optionalSource == null) {
			if (getSourceFileChooser().askUserForFile(originAction) == null) {
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

		isDeleteSourceAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "deleteSourceAfter");
		isOpenTargetFolderAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "openTargetFolder");
		isOpenAssociatedApplication = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false),
				"openAssociatedApplication");

		isDisableControls = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isDisableControls");
		isProgressVisible = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isProgressVisible");
		progressValue = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(0), "progressValue");
		progressNote = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "progressNote");
		progressHandler = new ProgressHandlerPmMixinImpl(isProgressVisible, progressValue, progressNote);
	}

	public SaveFileChooserDialog getTargetFileChooser() {
		if (targetFileChooser == null) {
			targetFileChooser = new SaveFileChooserDialog("action.chooseTargetFile", "action.choose", appProps,
					"DecryptionTargetChooser") {
				@Override
				protected String onDialogClosed(String filePathName, JFileChooser ofd) {
					String ret = super.onDialogClosed(filePathName, ofd);
					if (ret != null) {
						targetFile.setValueByOwner(ret);
					}
					return ret;
				}

				@Override
				protected void suggestTarget(JFileChooser ofd) {
					String sourceFileStr = sourceFile.getValue();
					if (StringUtils.hasText(targetFile.getValue())) {
						// Case 1: Based on current target
						use(ofd, targetFile.getValue());
					} else if (decryptionDialogParameters != null
							&& decryptionDialogParameters.getTargetFile() != null) {
						if (decryptionDialogParameters.getSourceFile().equals(sourceFileStr)) {
							// exact match
							use(ofd, decryptionDialogParameters.getTargetFile());
						} else {
							// case when suggested parameters are provided for
							// neighbor
							use(ofd, madeUpTargetFileName(FilenameUtils
									.getFullPathNoEndSeparator(decryptionDialogParameters.getTargetFile())));
						}
					} else if (StringUtils.hasText(sourceFileStr) && new File(sourceFileStr).exists()) {
						use(ofd, madeUpTargetFileName(FilenameUtils.getFullPathNoEndSeparator(sourceFileStr)));
					}
				}

				private void use(JFileChooser ofd, String filePathName) {
					ofd.setCurrentDirectory(new File(FilenameUtils.getFullPathNoEndSeparator(filePathName)));
					ofd.setSelectedFile(new File(filePathName));
				}
			};
		}
		return targetFileChooser;
	}

	private boolean doWeHaveKeysToDecryptWith() {
		if (!keyRingService.readKeys().isEmpty()) {
			return true;
		}
		UiUtils.messageBox(originAction, text("phrase.noKeysForDecryption"), text("term.attention"),
				MessageSeverity.WARNING);
		host.getActionToOpenCertificatesList().actionPerformed(originAction);
		if (keyRingService.readKeys().isEmpty()) {
			return false;
		}
		return true;
	}

	public ExistingFileChooserDialog getSourceFileChooser() {
		if (sourceFileChooser == null) {
			sourceFileChooser = new ExistingFileChooserDialog(appProps, SOURCE_FOLDER) {
				@Override
				protected void doFileChooserPostConstruct(JFileChooser ofd) {
					super.doFileChooserPostConstruct(ofd);

					ofd.setAcceptAllFileFilterUsed(false);
					ofd.addChoosableFileFilter(
							new FileNameExtensionFilter("Encrypted files (.gpg, .pgp, .asc)", EXTENSIONS));
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

			validationErrors.removeAll(ValidationErrorsUtils.findErrorsForField(FN_SOURCE_FILE, validationErrors));

			try {
				String sourceFileStr = sourceFile.getValue();
				if (!StringUtils.hasText(sourceFileStr)) {
					validationErrors.add(new FieldRequiredValidationError(FN_SOURCE_FILE));
					return;
				}

				if (!new File(sourceFileStr).exists()) {
					validationErrors.add(new ValidationError("error.thisFileDoesntExist", FN_SOURCE_FILE));
					return;
				}

				// Determine key and password here
				sourceFileRecipientsKeysIds = encryptionService.findKeyIdsForDecryption(sourceFileStr);
				if (keyAndPassword != null
						&& sourceFileRecipientsKeysIds.contains(keyAndPassword.getDecryptionKeyId())) {
					keyAndPasswordCallback.onKeyPasswordResult(keyAndPassword);
				} else {
					Message purpose = new Message("phrase.needKeyToDecryptFile",
							new Object[] { FilenameUtils.getName(sourceFileStr) });
					// Detour
					host.askUserForKeyAndPassword(sourceFileRecipientsKeysIds, purpose, keyAndPasswordCallback);
					// NOTE: That might lead to immediate sync call to
					// keyAndPasswordCallback if password was cached. If user
					// needs to be requested then new window will appear and
					// callback will be called later upon user input event
				}
			} catch (SymmetricEncryptionIsNotSupportedException sense) {
				log.warn("Got SymmetricEncryptionIsNotSupportedException", sense);
				validationErrors.add(new ValidationError(sense.getMessageCode(), FN_SOURCE_FILE));
			} catch (Throwable t) {
				log.error("Failed to find decryption keys", t);
				validationErrors.add(
						new ValidationError("error.failedToDetermineDecryptionMetodsForGivenFile", FN_SOURCE_FILE));
			} finally {
				updatePrimaryOperationAvailability();
			}
		}

		private KeyAndPasswordCallback keyAndPasswordCallback = (PasswordDeterminedForKey keyAndPassword) -> {
			try {
				DecryptOnePm.this.keyAndPassword = keyAndPassword;
				if (keyAndPassword == null) {
					validationErrors.add(new ValidationError("error.noMatchingKeysRegistered", FN_SOURCE_FILE));
					return;
				}

				String sourceFileStr = sourceFile.getValue();
				// Get target file name ("pre-decrypt")
				String targetFileName = encryptionService.getNameOfFileEncrypted(sourceFileStr, keyAndPassword);
				anticipatedTargetFileName = patchTargetFilenameIfNeeded(sourceFileStr, targetFileName);

				// Continue with source file change handling
				decryptionDialogParameters = findParamsBasedOnSourceFile(sourceFile.getValue());
				if (decryptionDialogParameters != null) {
					useSugestedParameters(decryptionDialogParameters);
				}
			} catch (Throwable t) {
				log.error("Failed to find decryption keys", t);
				validationErrors.add(
						new ValidationError("error.failedToDetermineDecryptionMetodsForGivenFile", FN_SOURCE_FILE));
			} finally {
				updatePrimaryOperationAvailability();
			}
		};

		private String patchTargetFilenameIfNeeded(String sourceFileStr, String targetFilename) {
			if (StringUtils.hasText(targetFilename)) {
				if (targetFilename.contains("/")) {
					targetFilename = targetFilename.substring(targetFilename.lastIndexOf("/") + 1);
				} else if (targetFilename.contains("\\")) {
					targetFilename = targetFilename.substring(targetFilename.lastIndexOf("\\") + 1);
				}
			}

			if (!StringUtils.hasText(targetFilename)) {
				targetFilename = FilenameUtils.getBaseName(sourceFileStr);
				log.warn("Wasn't able to find initial file name from " + sourceFileStr + ". Will use this name: "
						+ targetFilename);
			}
			return targetFilename;
		}

		protected DecryptionDialogParameters findParamsBasedOnSourceFile(String sourceFile) {
			DecryptionDialogParameters params = decryptionParams.find(sourceFile, null);
			if (params == null) {
				params = decryptionParams.find(FilenameUtils.getFullPathNoEndSeparator(sourceFile), null);
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
					targetFile.setValueByOwner(
							madeUpTargetFileName(FilenameUtils.getFullPathNoEndSeparator(params.getTargetFile())));
				}
			}
			// NOTE: MAGIC: We need to set it AFTER we set targetFolder. Because
			// then isUseBrowseFolder onChange handler will not open folder selection dialog
			if (params.isUseSameFolder()) {
				isUseTempFolder.setValueByOwner(false);
				isUseBrowseFolder.setValueByOwner(false);
				isUseSameFolder.setValueByOwner(true);
			} else if (params.isUseTempFolder()) {
				isUseBrowseFolder.setValueByOwner(false);
				isUseSameFolder.setValueByOwner(false);
				isUseTempFolder.setValueByOwner(true);
			} else {
				isUseSameFolder.setValueByOwner(false);
				isUseTempFolder.setValueByOwner(false);
				isUseBrowseFolder.setValueByOwner(true);
			}

			isDeleteSourceAfter.setValueByOwner(params.isDeleteSourceFile());
			isOpenTargetFolderAfter.setValueByOwner(params.isOpenTargetFolder());
			isOpenAssociatedApplication.setValueByOwner(params.isOpenAssociatedApplication());
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
				getTargetFileChooser().askUserForFile(UiUtils.actionEvent(evt));
			}

			if (!result) {
				clearValidationErrorsFromTargetFile();
			}
		}
	};

	protected void updatePrimaryOperationAvailability() {
		boolean result = true;
		result &= StringUtils.hasText(sourceFile.getValue()) && new File(sourceFile.getValue()).exists();
		result &= keyAndPassword != null;
		result &= validationErrors.size() == 0;
		actionDoOperation.setEnabled(result);
	}

	@SuppressWarnings("serial")
	protected final Action actionDoOperation = new LocalizedActionEx("action.decrypt", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			actionDoOperation.setEnabled(false);
			isDisableControls.setValueByOwner(true);
			operationThread = new Thread(new OperationWorker(e));
			operationThread.start();
		}
	};

	private class OperationWorker implements Runnable {
		private ActionEvent workerOriginEvent;

		public OperationWorker(ActionEvent workerOriginEvent) {
			this.workerOriginEvent = workerOriginEvent;
		}

		@Override
		public void run() {
			String targetFileName = getEffectiveTargetFileName();
			if (targetFileName == null) {
				actionDoOperation.setEnabled(true);
				isDisableControls.setValueByOwner(false);
				return;
			}

			String sourceFileStr = sourceFile.getValue();

			Fingerprint sourceFileFingerprint = null;
			Fingerprint targetFileFingerprint = null;
			Future<Fingerprint> sourceFileFingerprintFuture = null;
			try {
				// NOTE: In parallel we'll calculate checksum of the source. I hope IO caching
				// layer will handle 2 reading process for same file nicely and we won't have to
				// report progress on source checksum separately
				sourceFileFingerprintFuture = executorService
						.submit(new ChecksumCalculationTask(sourceFileStr, messageDigestFactory.createNew()));

				// and here is an actual decryption process
				ChecksumCalcOutputStreamSupervisor outputStreamSupervisor = new ChecksumCalcOutputStreamSupervisorImpl(
						messageDigestFactory);
				FileUtilsEx.baitAndSwitch(targetFileName, x -> encryptionService.decrypt(sourceFileStr, x,
						keyAndPassword, progressHandler, outputStreamSupervisor));
				log.debug("Decryption completed: " + targetFileName);

				// NOTE: We can calculate checksum for target file because we write it in full,
				// but input file is not really being read in full so we'll have to calculate
				// checksum of source file separately
				targetFileFingerprint = outputStreamSupervisor.getFingerprint();

				// Calculate source file CRC
				sourceFileFingerprint = sourceFileFingerprintFuture.get();

			} catch (UserRequestedCancellationException ce) {
				if (sourceFileFingerprintFuture != null) {
					sourceFileFingerprintFuture.cancel(true);
				}
				host.handleClose();
				return;
			} catch (Throwable t) {
				log.error("Failed to decrypt", t);
				if (sourceFileFingerprintFuture != null) {
					sourceFileFingerprintFuture.cancel(true);
				}
				EntryPoint.reportExceptionToUser(workerOriginEvent, "error.failedToDecryptFile", t);
				actionDoOperation.setEnabled(true);
				isDisableControls.setValueByOwner(false);
				return;
			}

			// Remember parameters
			persistDecryptionDialogParametersForCurrentInputs(targetFileName);
			persistEncryptionDialogParameters(targetFileName);
			addToMonitoredDecrypted(sourceFileStr, targetFileName, sourceFileFingerprint, targetFileFingerprint);

			// Delete source if asked
			if (isDeleteSourceAfter.getValue()) {
				try {
					Preconditions.checkState(new File(sourceFileStr).delete(), "File.delete() returned false");
				} catch (Throwable t) {
					EntryPoint.reportExceptionToUser(workerOriginEvent, "error.decryptOkButCantDeleteSource", t);
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
				UiUtils.messageBox(workerOriginEvent, text("phrase.decryptionSuccess", targetFileName),
						text("term.success"), MessageSeverity.INFO);
			}

			// close window
			host.handleClose();
		}

		private void addToMonitoredDecrypted(String sourceFileStr, String targetFileName,
				Fingerprint sourceFileFingerprint, Fingerprint targetFileFingerprint) {
			DecryptedFile decryptedFile = new DecryptedFile(sourceFileStr, targetFileName);
			decryptedFile.setEncryptedFileFingerprint(sourceFileFingerprint);
			decryptedFile.setDecryptedFileFingerprint(targetFileFingerprint);
			monitoringDecryptedFilesService.addOrUpdate(decryptedFile);
		}

		private void openAssociatedApp(String targetFileName) {
			try {
				Desktop.getDesktop().open(new File(targetFileName));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser(workerOriginEvent, "error.decryptOkButCantBrowseForFolder", t);
			}
		}

		private void browseForFolder(String targetFileName) {
			try {
				Desktop.getDesktop().browse(new File(targetFileName).getParentFile().toURI());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser(workerOriginEvent, "error.decryptOkButCantBrowseForFolder", t);
			}
		}

		private String getEffectiveTargetFileName() {
			if (isUseSameFolder.getValue()) {
				return madeUpTargetFileName(FilenameUtils.getFullPathNoEndSeparator(sourceFile.getValue()));
			} else if (isUseTempFolder.getValue()) {
				return getEffectiveFileNameForTempFolder();
			}

			// Validation for target folder!! ---
			if (!validateTargetFile()) {
				return null;
			}

			String ret = targetFile.getValue();
			File parentFolder = new File(ret).getParentFile();
			Preconditions.checkState(parentFolder.exists() || parentFolder.mkdirs(),
					"Failed to ensure all parents directories created");
			return ret;
		}

		private String getEffectiveFileNameForTempFolder() {
			DecryptedFile dfm = monitoringDecryptedFilesService.findByEncryptedFile(getSourceFile().getValue(),
					x -> x.getDecryptedFile().startsWith(decryptedTempFolder.getTempFolderBasePath()));
			if (dfm == null) {
				String ret = madeUpTargetFileName(decryptedTempFolder.getTempFolderBasePath());
				return FileUtilsEx.ensureFileNameVacant(ret);
			}

			// Suggest to open instead of overwrite
			String msg = text("warning.fileWasAlreadyDecryptedIntoTempFolder",
					new Object[] { dfm.getEncryptedFile(), dfm.getDecryptedFile() });
			int response = JOptionPane.showConfirmDialog(findRegisteredWindowIfAny(), UiUtils.getMultilineMessage(msg),
					text("term.confirmation"), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

			if (response == JOptionPane.CANCEL_OPTION) {
				return null;
			} else if (response == JOptionPane.NO_OPTION) {
				// overwrite
				return dfm.getDecryptedFile();
			} else {
				// NOTE: That is a nasty violation of something. This method should not control
				// PM workflow like this. But today I don't feel myself ok, I hope you will
				// forgive me. Will refactor it later.
				if (isOpenTargetFolderAfter.getValue()) {
					browseForFolder(dfm.getDecryptedFile());
				} else {
					openAssociatedApp(dfm.getDecryptedFile());
				}
				host.handleClose();
				return null;
			}
		}

		private void persistDecryptionDialogParametersForCurrentInputs(String targetFile) {
			DecryptionDialogParameters dialogParameters = buildDecryptionDialogParameters(targetFile);
			decryptionParams.put(dialogParameters.getSourceFile(), dialogParameters);
			decryptionParams.put(FilenameUtils.getFullPathNoEndSeparator(dialogParameters.getSourceFile()),
					dialogParameters);
		}

		/**
		 * We use this method to store parameters that program will suggest to use when
		 * user will desire to encrypt back file that was just decrypted
		 * 
		 * NOTE: Slight SRP concern here. We're interfering with responsibility area of
		 * {@link EncryptOnePm}
		 * 
		 * @param decryptedFile
		 *            decrypted file path name
		 */
		protected void persistEncryptionDialogParameters(String decryptedFile) {
			EncryptionDialogParameters dialogParameters = buildEncryptionDialogParameters(decryptedFile);
			encryptionParamsStorage.persistDialogParametersForCurrentInputs(dialogParameters, false);
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

		private DecryptionDialogParameters buildDecryptionDialogParameters(String targetFile) {
			DecryptionDialogParameters ret = new DecryptionDialogParameters();
			ret.setSourceFile(sourceFile.getValue());
			ret.setUseSameFolder(isUseSameFolder.getValue());
			ret.setUseTempFolder(isUseTempFolder.getValue());
			ret.setTargetFile(targetFile);
			ret.setDecryptionKeyId(keyAndPassword.getDecryptionKeyId());
			ret.setDeleteSourceFile(isDeleteSourceAfter.getValue());
			ret.setOpenTargetFolder(isOpenTargetFolderAfter.getValue());
			ret.setOpenAssociatedApplication(isOpenAssociatedApplication.getValue());
			ret.setCreatedAt(System.currentTimeMillis());
			return ret;
		}
	};

	private String madeUpTargetFileName(String targetBasedPath) {
		return targetBasedPath + File.separator + anticipatedTargetFileName;
	}

	@SuppressWarnings("serial")
	protected final Action actionCancel = new LocalizedActionEx("action.cancel", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			if (operationThread != null && operationThread.isAlive()) {
				operationThread.interrupt();
			} else {
				host.handleClose();
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionBrowseSource = new LocalizedActionEx("action.browse", "DecryptOnePm.source") {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			getSourceFileChooser().askUserForFile(e);
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionBrowseTarget = new LocalizedActionEx("action.browse", "DecryptOnePm.target") {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			getTargetFileChooser().askUserForFile(e);
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

	public static boolean isItLooksLikeYourSourceFile(String file) {
		return new File(file).exists()
				&& containsIgnoreCase(EXTENSIONS, FilenameUtils.getExtension(file).toLowerCase());
	}

	private static boolean containsIgnoreCase(String[] arr, String subject) {
		for (String s : arr) {
			if (s.equalsIgnoreCase(subject)) {
				return true;
			}
		}
		return false;
	}

	public ModelPropertyAccessor<Boolean> getIsProgressVisible() {
		return isProgressVisible.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Integer> getProgressValue() {
		return progressValue.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getProgressNote() {
		return progressNote.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getIsDisableControls() {
		return isDisableControls.getModelPropertyAccessor();
	}

}
