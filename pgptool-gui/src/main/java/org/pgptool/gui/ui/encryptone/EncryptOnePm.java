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
package org.pgptool.gui.ui.encryptone;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;
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
import org.pgptool.gui.bkgoperation.ProgressHandler;
import org.pgptool.gui.bkgoperation.UserReqeustedCancellationException;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.encryptionparams.api.EncryptionParamsStorage;
import org.pgptool.gui.ui.decryptone.DecryptOnePm;
import org.pgptool.gui.ui.keyslist.ComparatorKeyByNameImpl;
import org.pgptool.gui.ui.tools.ListChangeListenerAnyEventImpl;
import org.pgptool.gui.ui.tools.ProgressHandlerPmMixinImpl;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ExistingFileChooserDialog;
import org.pgptool.gui.ui.tools.browsefs.SaveFileChooserDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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

	@Autowired
	@Qualifier("appProps")
	private ConfigPairs appProps;
	@Autowired
	private EncryptionParamsStorage encryptionParamsStorage;

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
	private SaveFileChooserDialog targetFileChooser;
	private EncryptionDialogParameters encryptionDialogParameters;

	private ModelProperty<Boolean> isProgressVisible;
	private ModelProperty<Integer> progressValue;
	private ModelProperty<String> progressNote;
	private ModelProperty<Boolean> isDisableControls;
	private ProgressHandler progressHandler;
	private Thread operationThread;

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

	public SaveFileChooserDialog getTargetFileChooser() {
		if (targetFileChooser == null) {
			targetFileChooser = new SaveFileChooserDialog(findRegisteredWindowIfAny(), "action.chooseTargetFile",
					"action.choose", appProps, "EncryptionTargetChooser") {
				@Override
				protected String onDialogClosed(String filePathName, JFileChooser ofd) {
					String ret = super.onDialogClosed(filePathName, ofd);
					if (ret != null) {
						targetFile.setValueByOwner(ret);
					}
					return ret;
				}

				@Override
				protected void onFileChooserPostConstrct(JFileChooser ofd) {
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
							use(ofd, madeUpTargetFileName(sourceFile.getValue(), FilenameUtils
									.getFullPathNoEndSeparator(encryptionDialogParameters.getTargetFile())));
						}
					} else if (StringUtils.hasText(sourceFileStr) && new File(sourceFileStr).exists()) {
						String basePath = FilenameUtils.getFullPathNoEndSeparator(sourceFileStr);
						ofd.setCurrentDirectory(new File(basePath));
						ofd.setSelectedFile(new File(madeUpTargetFileName(sourceFileStr, basePath)));
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
		UiUtils.messageBox(text("phrase.noKeysForEncryption"), text("term.attention"), MessageSeverity.WARNING);
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
		targetFile.getModelPropertyAccessor().addPropertyChangeListener(onTargetFileChanged);
		targetFileEnabled = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "targetFile");

		List<Key<KeyData>> allKeys = keyRingService.readKeys();
		allKeys.sort(new ComparatorKeyByNameImpl<>());
		availabileRecipients = new ModelListProperty<Key<KeyData>>(this,
				new ValueAdapterReadonlyImpl<List<Key<KeyData>>>(allKeys), "availabileRecipients");
		selectedRecipients = new ModelMultSelInListProperty<Key<KeyData>>(this,
				new ValueAdapterHolderImpl<List<Key<KeyData>>>(new ArrayList<Key<KeyData>>()), "projects",
				availabileRecipients);
		selectedRecipients.getModelMultSelInListPropertyAccessor().addListExEventListener(onRecipientsSelectionChanged);
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
			sourceFileChooser = new ExistingFileChooserDialog(findRegisteredWindowIfAny(), appProps, SOURCE_FOLDER) {
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
				useSugestedParameters(encryptionDialogParameters);
			} else {
				selectSelfAsRecipient();
			}
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

			Set<String> missedKeys = preselectRecipients(new HashSet<>(params.getRecipientsKeysIds()));
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

		private void notifyUserOfMissingKeysIfAny(Set<String> missedKeys) {
			if (CollectionUtils.isEmpty(missedKeys)) {
				return;
			}

			UiUtils.messageBox(findRegisteredWindowIfAny(),
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
						getTargetFileChooser().askUserForFile();
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
			actionDoOperation.setEnabled(false);
			isDisableControls.setValueByOwner(true);
			operationThread = new Thread(operationWorker);
			operationThread.start();
		}
	};

	private Runnable operationWorker = new Runnable() {
		@Override
		public void run() {
			String targetFileName = getEffectiveTargetFileName();

			try {
				encryptionService.encrypt(sourceFile.getValue(), targetFileName, selectedRecipients.getList(),
						progressHandler);
			} catch (UserReqeustedCancellationException ce) {
				host.handleClose();
				return;
			} catch (Throwable t) {
				log.error("Failed to encrypt", t);
				EntryPoint.reportExceptionToUser("error.failedToEncryptFile", t);
				actionDoOperation.setEnabled(true);
				isDisableControls.setValueByOwner(false);
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
				UiUtils.messageBox(text("phrase.encryptionSuccess"), text("term.success"), MessageSeverity.INFO);
			}

			// Remember parameters
			encryptionParamsStorage.persistDialogParametersForCurrentInputs(buildEncryptionDialogParameters(), true);

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
				return madeUpTargetFileName(sourceFile.getValue(),
						FilenameUtils.getFullPathNoEndSeparator(sourceFile.getValue()));
			}

			String targetFileName = targetFile.getValue();
			File parentFolder = new File(targetFileName).getParentFile();
			Preconditions.checkState(parentFolder.exists() || parentFolder.mkdirs(),
					"Failed to ensure all parents directories created");
			return targetFileName;
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
			if (operationThread != null && operationThread.isAlive()) {
				operationThread.interrupt();
			} else {
				host.handleClose();
			}
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
			getTargetFileChooser().askUserForFile();
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
