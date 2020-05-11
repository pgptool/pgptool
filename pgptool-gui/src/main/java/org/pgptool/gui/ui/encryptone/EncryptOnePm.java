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
package org.pgptool.gui.ui.encryptone;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.bkgoperation.Progress;
import org.pgptool.gui.bkgoperation.Progress.Updater;
import org.pgptool.gui.bkgoperation.ProgressHandler;
import org.pgptool.gui.bkgoperation.UserRequestedCancellationException;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;
import org.pgptool.gui.decryptedlist.api.MonitoringDecryptedFilesService;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryptionparams.api.EncryptionParamsStorage;
import org.pgptool.gui.filecomparison.ChecksumCalcInputStreamSupervisor;
import org.pgptool.gui.filecomparison.ChecksumCalcInputStreamSupervisorImpl;
import org.pgptool.gui.filecomparison.ChecksumCalcOutputStreamSupervisor;
import org.pgptool.gui.filecomparison.ChecksumCalcOutputStreamSupervisorImpl;
import org.pgptool.gui.filecomparison.Fingerprint;
import org.pgptool.gui.filecomparison.MessageDigestFactory;
import org.pgptool.gui.tools.FileUtilsEx;
import org.pgptool.gui.ui.decryptone.DecryptOnePm;
import org.pgptool.gui.ui.keyslist.ComparatorKeyByNameImpl;
import org.pgptool.gui.ui.tools.ListChangeListenerAnyEventImpl;
import org.pgptool.gui.ui.tools.ProgressHandlerPmMixinImpl;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ExistingFileChooserDialog;
import org.pgptool.gui.ui.tools.browsefs.SaveFileChooserDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.summerb.utils.objectcopy.DeepCopy;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.EXPORT.base.LocalizedActionEx;
import ru.skarpushin.swingpm.EXPORT.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.lists.ModelListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultSelInListProperty;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class EncryptOnePm extends PresentationModelBase<EncryptOneHost, String> {
	private static Logger log = Logger.getLogger(EncryptOnePm.class);

	private static final String ENCRYPTED_FILE_EXTENSION = "pgp";
	private static final String SOURCE_FOLDER = "EncryptOnePm.SOURCE_FOLDER";

	@Autowired
	private ConfigPairs appProps;
	@Autowired
	private EncryptionParamsStorage encryptionParamsStorage;
	@Autowired
	private MessageDigestFactory messageDigestFactory;
	@Autowired
	private MonitoringDecryptedFilesService monitoringDecryptedFilesService;
	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private EncryptionService encryptionService;

	private ModelProperty<String> sourceFile;
	private ModelProperty<Boolean> isUseSameFolder;
	private ModelProperty<String> targetFile;
	private ModelProperty<Boolean> targetFileEnabled;
	private ModelMultSelInListProperty<Key> selectedRecipients;
	private ModelListProperty<Key> availabileRecipients;
	private ModelProperty<Boolean> isNoPrivateKeysSelected;
	private ModelProperty<Boolean> isDeleteSourceAfter;
	private ModelProperty<Boolean> isOpenTargetFolderAfter;

	private ExistingFileChooserDialog sourceFileChooser;
	private SaveFileChooserDialog targetFileChooser;
	private EncryptionDialogParameters encryptionDialogParameters;

	private ModelProperty<Boolean> isProgressVisible;
	private ModelProperty<Integer> progressValue;
	private ModelProperty<String> progressNote;
	private ModelProperty<Boolean> isDisableControls;
	private ProgressHandler progressHandler;
	private Thread operationThread;

	@Override
	public boolean init(ActionEvent originAction, EncryptOneHost host, String optionalSource) {
		super.init(originAction, host, optionalSource);
		Preconditions.checkArgument(host != null);

		if (!doWeHaveKeysToEncryptWith()) {
			return false;
		}

		initModelProperties();

		if (optionalSource == null) {
			if (askUserForSourceFile(originAction) == null) {
				return false;
			}
		} else {
			sourceFile.setValueByOwner(optionalSource);
		}

		return true;
	}

	private String askUserForSourceFile(ActionEvent originEvent) {
		String selectedSourceFile;
		if ((selectedSourceFile = getSourceFileChooser().askUserForFile(originEvent)) == null) {
			return null;
		}
		sourceFile.setValueByOwner(selectedSourceFile);
		return selectedSourceFile;
	}

	public SaveFileChooserDialog getTargetFileChooser() {
		if (targetFileChooser == null) {
			targetFileChooser = new SaveFileChooserDialog("action.chooseTargetFile", "action.choose", appProps,
					"EncryptionTargetChooser") {
				@Override
				protected String onDialogClosed(String filePathName, JFileChooser ofd) {
					String ret = super.onDialogClosed(filePathName, ofd);
					if (ret != null) {
						targetFile.setValueByOwner(ret);
					}
					return ret;
				}

				@Override
				protected void onFileChooserPostConstruct(JFileChooser ofd) {
					ofd.setAcceptAllFileFilterUsed(false);
					ofd.addChoosableFileFilter(new FileNameExtensionFilter("GPG Files (.pgp)", "pgp"));
					// NOTE: Should we support other extensions?....
					ofd.addChoosableFileFilter(ofd.getAcceptAllFileFilter());
					ofd.setFileFilter(ofd.getChoosableFileFilters()[0]);
				}

				@Override
				protected void suggestTarget(JFileChooser ofd) {
					String sourceFileStr = sourceFile.getValue();
					if (StringUtils.hasText(targetFile.getValue())) {
						use(ofd, targetFile.getValue());
					} else if (encryptionDialogParameters != null
							&& encryptionDialogParameters.getTargetFile() != null) {
						if (encryptionDialogParameters.getSourceFile().equals(sourceFile.getValue())) {
							use(ofd, encryptionDialogParameters.getTargetFile());
						} else {
							use(ofd, makeUpTargetFileName(sourceFile.getValue(), FilenameUtils
									.getFullPathNoEndSeparator(encryptionDialogParameters.getTargetFile())));
						}
					} else if (StringUtils.hasText(sourceFileStr) && new File(sourceFileStr).exists()) {
						String basePath = FilenameUtils.getFullPathNoEndSeparator(sourceFileStr);
						ofd.setCurrentDirectory(new File(basePath));
						ofd.setSelectedFile(new File(makeUpTargetFileName(sourceFileStr, basePath)));
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

	private boolean doWeHaveKeysToEncryptWith() {
		if (!keyRingService.readKeys().isEmpty()) {
			return true;
		}
		UiUtils.messageBox(originAction, text("phrase.noKeysForEncryption"), text("term.attention"),
				MessageSeverity.WARNING);
		host.getActionToOpenCertificatesList().actionPerformed(originAction);
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
		targetFile.getModelPropertyAccessor().addPropertyChangeListener(onTargetFileChanged);
		targetFileEnabled = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "targetFile");

		List<Key> allKeys = keyRingService.readKeys();
		allKeys.sort(new ComparatorKeyByNameImpl());
		availabileRecipients = new ModelListProperty<Key>(this, new ValueAdapterReadonlyImpl<List<Key>>(allKeys),
				"availabileRecipients");
		selectedRecipients = new ModelMultSelInListProperty<Key>(this,
				new ValueAdapterHolderImpl<List<Key>>(new ArrayList<Key>()), "projects", availabileRecipients);
		selectedRecipients.getModelMultSelInListPropertyAccessor().addListExEventListener(onRecipientsSelectionChanged);
		isNoPrivateKeysSelected = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "isNoPrivateKeysSelected");
		onRecipientsSelectionChanged.onListChanged();

		isDeleteSourceAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "deleteSourceAfter");
		isOpenTargetFolderAfter = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "openTargetFolder");

		onUseSameFolderChanged.propertyChange(null);

		isDisableControls = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isDisableControls");
		isProgressVisible = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isProgressVisible");
		progressValue = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(0), "progressValue");
		progressNote = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "progressNote");
		progressHandler = new ProgressHandlerPmMixinImpl(isProgressVisible, progressValue, progressNote);
	}

	private PropertyChangeListener onTargetFileChanged = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			refreshPrimaryOperationAvailability();
		}
	};

	public ExistingFileChooserDialog getSourceFileChooser() {
		if (sourceFileChooser == null) {
			sourceFileChooser = new ExistingFileChooserDialog(appProps, SOURCE_FOLDER) {
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

			encryptionDialogParameters = encryptionParamsStorage.findParamsBasedOnSourceFile(sourceFile.getValue(),
					true);
			if (encryptionDialogParameters != null) {
				useSugestedParameters(encryptionDialogParameters, evt);
			} else {
				selectSelfAsRecipient();
			}
		}

		private void selectSelfAsRecipient() {
			selectedRecipients.getList().clear();
			for (Key key : availabileRecipients.getList()) {
				if (!key.getKeyData().isCanBeUsedForDecryption()) {
					continue;
				}
				selectedRecipients.getList().add(key);
			}
		}

		private void useSugestedParameters(EncryptionDialogParameters params, PropertyChangeEvent evt) {
			useSuggestedTargetFile(params);

			// NOTE: MAGIC: We need to set it AFTER we set targetFolder. Because
			// then isUseSameFolder onChange handler will not open folder
			// selection
			// dialog
			isUseSameFolder.setValueByOwner(params.isUseSameFolder());
			isDeleteSourceAfter.setValueByOwner(params.isDeleteSourceFile());
			isOpenTargetFolderAfter.setValueByOwner(params.isOpenTargetFolder());

			Set<String> missedKeys = preselectRecipients(new HashSet<>(params.getRecipientsKeysIds()));
			notifyUserOfMissingKeysIfAny(missedKeys, evt);
		}

		private void useSuggestedTargetFile(EncryptionDialogParameters params) {
			if (!params.isUseSameFolder()) {
				if (params.getSourceFile().equals(sourceFile.getValue())) {
					targetFile.setValueByOwner(params.getTargetFile());
				} else {
					// NOTE: Assuming that params fully valid and target file is
					// provided
					targetFile.setValueByOwner(makeUpTargetFileName(sourceFile.getValue(),
							FilenameUtils.getFullPathNoEndSeparator(params.getTargetFile())));
				}
			} else {
				targetFile.setValueByOwner("");
			}
		}

		private Set<String> preselectRecipients(Set<String> recipientsKeysIds) {
			selectedRecipients.getList().clear();
			Set<String> missedKeys = new HashSet<>();
			for (String keyId : recipientsKeysIds) {
				Optional<Key> key = availabileRecipients.getList().stream()
						.filter(x -> x.getKeyData().isHasAlternativeId(keyId)).findFirst();
				if (key.isPresent()) {
					selectedRecipients.getList().add(key.get());
				} else {
					missedKeys.add(keyId);
				}
			}
			return missedKeys;
		}

		private void notifyUserOfMissingKeysIfAny(Set<String> missedKeys, PropertyChangeEvent evt) {
			if (CollectionUtils.isEmpty(missedKeys)) {
				return;
			}

			UiUtils.messageBox(UiUtils.actionEvent(evt),
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
			refreshPrimaryOperationAvailability();
			if (result && !StringUtils.hasText(targetFile.getValue())) {
				// NOTE: We need to let event processing go otherwise it all be
				// messed up
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						getTargetFileChooser().askUserForFile(UiUtils.actionEvent(evt));
						refreshPrimaryOperationAvailability();
					}
				});
			}
		}
	};

	protected void refreshPrimaryOperationAvailability() {
		boolean result = true;
		result &= !selectedRecipients.getList().isEmpty();
		result &= StringUtils.hasText(sourceFile.getValue()) && new File(sourceFile.getValue()).exists();
		result &= isUseSameFolder.getValue() || StringUtils.hasText(targetFile.getValue());
		actionDoOperation.setEnabled(result);
	}

	private ListChangeListenerAnyEventImpl<Key> onRecipientsSelectionChanged = new ListChangeListenerAnyEventImpl<Key>() {
		@Override
		public void onListChanged() {
			refreshPrimaryOperationAvailability();

			isNoPrivateKeysSelected.setValueByOwner(selectedRecipients.getList().size() == 0 || selectedRecipients
					.getList().stream().filter(x -> x.getKeyData().isCanBeUsedForDecryption()).count() == 0);
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionDoOperation = new LocalizedActionEx("action.encrypt", this) {
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
			String targetFileName;
			Fingerprint source;
			Fingerprint target;

			try {
				targetFileName = getEffectiveTargetFileName();

				ChecksumCalcInputStreamSupervisor inputStreamSupervisor = new ChecksumCalcInputStreamSupervisorImpl(
						messageDigestFactory);
				ChecksumCalcOutputStreamSupervisor outputStreamSupervisor = new ChecksumCalcOutputStreamSupervisorImpl(
						messageDigestFactory);
				if (isEncryptedFileChangedAfterDecryption(targetFileName, sourceFile.getValue(), progressHandler)
						&& !promptUserToOverwriteConcurrentChanges(targetFileName)) {
					throw new UserRequestedCancellationException();
				}

				FileUtilsEx.baitAndSwitch(targetFileName, x -> encryptionService.encrypt(sourceFile.getValue(), x,
						selectedRecipients.getList(), progressHandler, inputStreamSupervisor, outputStreamSupervisor));

				source = inputStreamSupervisor.getFingerprint();
				target = outputStreamSupervisor.getFingerprint();

				log.debug("Encryption completed: " + targetFileName);
			} catch (UserRequestedCancellationException ce) {
				if (ce.isSoftCancel()) {
					reEnableDialog();
				} else {
					host.handleClose();
				}
				return;
			} catch (Throwable t) {
				log.error("Failed to encrypt", t);
				EntryPoint.reportExceptionToUser(workerOriginEvent, "error.failedToEncryptFile", t);
				reEnableDialog();
				return;
			}

			// Delete source if asked
			if (isDeleteSourceAfter.getValue()) {
				try {
					Preconditions.checkState(new File(sourceFile.getValue()).delete(), "File.delete() returned false");
				} catch (Throwable t) {
					EntryPoint.reportExceptionToUser(workerOriginEvent, "error.encryptOkButCantDeleteSource", t);
				}
			} else {
				updateBaselineFingerprintsIfApplicable(sourceFile.getValue(), targetFileName, source, target);
			}

			// Open target folder
			if (isOpenTargetFolderAfter.getValue()) {
				askOperSystemToBrowseForFolder(targetFileName);
			} else {
				// Or show confirmation
				UiUtils.messageBox(workerOriginEvent, text("phrase.encryptionSuccess"), text("term.success"),
						MessageSeverity.INFO);
			}

			// Remember parameters
			encryptionDialogParameters = buildEncryptionDialogParameters();
			encryptionParamsStorage.persistDialogParametersForCurrentInputs(encryptionDialogParameters, true);

			// close window
			host.handleClose();
		}

		private void reEnableDialog() {
			actionDoOperation.setEnabled(true);
			isDisableControls.setValueByOwner(false);
			isProgressVisible.setValueByOwner(false);
		}

		private boolean promptUserToOverwriteConcurrentChanges(String targetFileName) {
			return UiUtils.confirmRegular(workerOriginEvent, "confirm.doYouWantToOverwriteConcurrentChanges",
					new String[] { targetFileName });
		}

		private boolean isEncryptedFileChangedAfterDecryption(String encryptedFileName, String decryptedFilename,
				ProgressHandler progressHandler) throws IOException, UserRequestedCancellationException {
			File targetFileObj = new File(encryptedFileName);
			if (!targetFileObj.exists()) {
				return false;
			}

			DecryptedFile decryptedMonitored = monitoringDecryptedFilesService.findByDecryptedFile(decryptedFilename);
			if (decryptedMonitored == null) {
				return false;
			}

			if (decryptedMonitored.getEncryptedFileFingerprint() == null) {
				return false;
			}

			ChecksumCalcInputStreamSupervisor inputStreamSupervisor = new ChecksumCalcInputStreamSupervisorImpl(
					messageDigestFactory);
			try (InputStream inputStream = inputStreamSupervisor.get(encryptedFileName)) {
				Updater progress = Progress.create("operation.calculatingChecksum", progressHandler);
				BigInteger fileSize = BigInteger.valueOf(targetFileObj.length());
				progress.updateTotalSteps(fileSize);
				byte[] buf = new byte[4096];
				int read, totalRead = 0;
				while ((read = inputStream.read(buf)) > 0) {
					totalRead += read;
					if (progress.isCancelationRequested()) {
						throw new UserRequestedCancellationException();
					}
					progress.updateStepsTaken(BigInteger.valueOf(totalRead));
				}
			}

			Fingerprint initialFingerprint = decryptedMonitored.getEncryptedFileFingerprint();
			Fingerprint currentFingerprint = inputStreamSupervisor.getFingerprint();
			return !initialFingerprint.equals(currentFingerprint);
		}

		private void updateBaselineFingerprintsIfApplicable(String decryptedFileName, String encryptedFileName,
				Fingerprint source, Fingerprint target) {
			DecryptedFile decryptedMonitored = monitoringDecryptedFilesService.findByDecryptedFile(decryptedFileName);
			if (decryptedMonitored == null) {
				return;
			}

			DecryptedFile newDecryptedFile = DeepCopy.copy(decryptedMonitored);
			newDecryptedFile.setDecryptedFileFingerprint(source);
			newDecryptedFile.setEncryptedFileFingerprint(target);
			monitoringDecryptedFilesService.addOrUpdate(newDecryptedFile);
		}

		private void askOperSystemToBrowseForFolder(String targetFileName) {
			try {
				Desktop.getDesktop().browse(new File(targetFileName).getParentFile().toURI());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser(workerOriginEvent, "error.encryptOkButCantBrowseForFolder", t);
			}
		}

		private String getEffectiveTargetFileName() throws UserRequestedCancellationException {
			if (StringUtils.hasText(targetFile.getValue()) && !isUseSameFolder.getValue()) {
				String targetFileName = targetFile.getValue();
				File parentFolder = new File(targetFileName).getParentFile();
				Preconditions.checkState(parentFolder.exists() || parentFolder.mkdirs(),
						"Failed to ensure all parents directories created");
				return targetFileName;
			}

			// NOTE: Following logic is not triggered when user is encrypting back, because
			// encrypt back uses concrete target file location

			isUseSameFolder.setValueByOwner(true); // in case targetFile is empty, reinforce UseSameFolder
			String targetFileName = makeUpTargetFileName(sourceFile.getValue(),
					FilenameUtils.getFullPathNoEndSeparator(sourceFile.getValue()));

			boolean confirmOverwriteWithUser = (encryptionDialogParameters == null
					|| !sourceFile.getValue().equalsIgnoreCase(encryptionDialogParameters.getSourceFile()))
					&& new File(targetFileName).exists();
			if (confirmOverwriteWithUser && !promptUserToOverwriteExistingFile(targetFileName)) {
				throw new UserRequestedCancellationException(true);
			}
			return targetFileName;
		}

		private boolean promptUserToOverwriteExistingFile(String targetFileName) {
			return UiUtils.confirmWarning(workerOriginEvent, "confirm.overWriteExistingFile",
					new Object[] { targetFileName });
		}

		private EncryptionDialogParameters buildEncryptionDialogParameters() {
			EncryptionDialogParameters ret = new EncryptionDialogParameters();
			ret.setSourceFile(sourceFile.getValue());
			ret.setUseSameFolder(isUseSameFolder.getValue());
			ret.setTargetFile(targetFile.getValue());
			ret.setDeleteSourceFile(isDeleteSourceAfter.getValue());
			ret.setOpenTargetFolder(isOpenTargetFolderAfter.getValue());
			ret.setRecipientsKeysIds(new ArrayList<>(selectedRecipients.getList().size()));
			for (Key key : selectedRecipients.getList()) {
				ret.getRecipientsKeysIds().add(key.getKeyInfo().getKeyId());
			}
			return ret;
		}
	};

	protected static String makeUpTargetFileName(String sourceFileName, String targetBasedPath) {
		String fileNameOnlyWoPathAndExtension = FilenameUtils.getBaseName(sourceFileName);
		return targetBasedPath + File.separator + fileNameOnlyWoPathAndExtension + "." + ENCRYPTED_FILE_EXTENSION;
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
	protected final Action actionBrowseSource = new LocalizedActionEx("action.browse", "EncryptOnePm.source") {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			askUserForSourceFile(e);
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionBrowseTarget = new LocalizedActionEx("action.browse", "EncryptOnePm.target") {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			getTargetFileChooser().askUserForFile(e);
			refreshPrimaryOperationAvailability();
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

	public ModelMultSelInListProperty<Key> getSelectedRecipients() {
		// NOTE: I did sort of exception here. Instead of providing accessor I'm
		// prvoding model property itself so that JCheckList can bind directly to list
		// of checked recipients
		// TBD: This doesn't look right actually, I'd say it's better to refactor it
		// and incorporate changes into swingpm if possible
		return selectedRecipients;
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

	public ModelPropertyAccessor<Boolean> getIsNoPrivateKeysSelected() {
		return isNoPrivateKeysSelected.getModelPropertyAccessor();
	}
}
