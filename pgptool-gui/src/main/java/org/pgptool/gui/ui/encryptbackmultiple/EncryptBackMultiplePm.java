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
package org.pgptool.gui.ui.encryptbackmultiple;

import static org.pgptool.gui.app.Messages.text;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JOptionPane;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.bkgoperation.Progress;
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
import org.pgptool.gui.filecomparison.ChecksumCalculationTask;
import org.pgptool.gui.filecomparison.Fingerprint;
import org.pgptool.gui.filecomparison.MessageDigestFactory;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.tools.FileUtilsEx;
import org.pgptool.gui.ui.encryptone.EncryptionDialogParameters;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ValueAdapterPersistentPropertyImpl;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.dto.EncryptBackAllIterationUsage;
import org.pgptool.gui.usage.dto.EncryptBackAllUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.utils.objectcopy.DeepCopy;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class EncryptBackMultiplePm
    extends PresentationModelBaseEx<EncryptBackMultipleHost, Set<String>> {
  private static Logger log = Logger.getLogger(EncryptBackMultiplePm.class);

  @Autowired private EncryptionParamsStorage encryptionParamsStorage;
  @Autowired private ConfigPairs appProps;
  @Autowired private KeyRingService keyRingService;
  @Autowired private EncryptionService encryptionService;
  @Autowired private MonitoringDecryptedFilesService monitoringDecryptedFilesService;
  @Autowired private MessageDigestFactory messageDigestFactory;
  @Autowired private UsageLogger usageLogger;

  private Map<String, EncryptionDialogParameters> mapFileToEncryptionParams = new HashMap<>();

  private ModelProperty<String> sourceFilesSummary;
  private ModelProperty<String> recipientsSummary;
  private ModelProperty<Boolean> isHasMissingRecipients;
  private ModelProperty<Boolean> isIgnoreMissingRecipientsWarning;
  private ModelProperty<Boolean> isEncryptOnlyChanged;
  private ModelProperty<Boolean> isDeleteSourceAfter;

  private ModelProperty<Boolean> isProgressVisible;
  private ModelProperty<Integer> progressValue;
  private ModelProperty<String> progressNote;
  private ModelProperty<Boolean> isDisableControls;

  @Override
  public boolean init(
      ActionEvent originAction, EncryptBackMultipleHost host, Set<String> decryptedFiles) {
    super.init(originAction, host, decryptedFiles);
    Preconditions.checkArgument(host != null, "host required");
    Preconditions.checkArgument(
        decryptedFiles != null && decryptedFiles.size() > 0,
        "decryptedFiles array must not be empty");

    if (!doWeHaveKeysToEncryptWith()) {
      return false;
    }

    initModelProperties();
    return true;
  }

  private boolean doWeHaveKeysToEncryptWith() {
    if (!keyRingService.readKeys().isEmpty()) {
      return true;
    }
    UiUtils.messageBox(
        originAction,
        text("phrase.noKeysForEncryption"),
        text("term.attention"),
        MessageSeverity.WARNING);
    host.getActionToOpenCertificatesList().actionPerformed(originAction);
    if (keyRingService.readKeys().isEmpty()) {
      return false;
    }
    return true;
  }

  private void initModelProperties() {
    isDeleteSourceAfter =
        new ModelProperty<>(
            this,
            new ValueAdapterPersistentPropertyImpl<>(
                appProps, "EncryptBackMultiplePm.isDeleteUnencryptedAfter", true),
            "isDeleteUnencryptedAfter");
    isHasMissingRecipients =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isHasMissingRecipients");
    isIgnoreMissingRecipientsWarning =
        new ModelProperty<>(
            this, new ValueAdapterHolderImpl<>(false), "isIgnoreMissingRecipientsWarning");
    isEncryptOnlyChanged =
        new ModelProperty<>(
            this,
            new ValueAdapterPersistentPropertyImpl<>(
                appProps, "EncryptBackMultiplePm.isEncryptOnlyChanged", true),
            "isEncryptOnlyChanged");

    sourceFilesSummary =
        new ModelProperty<>(
            this,
            new ValueAdapterHolderImpl<>(text("encrypBackMany.operationInfo", initParams.size())),
            "sourceFilesSummary");
    recipientsSummary =
        new ModelProperty<>(
            this, new ValueAdapterHolderImpl<>("(collecting information...)"), "recipientsSummary");

    isHasMissingRecipients.setValueByOwner(discoverIsHasMissingRecipients());
    if (!isHasMissingRecipients.getValue()) {
      recipientsSummary.setValueByOwner(text("encrypBackMany.willBeEncryptedForAllSameRecipients"));
    } else {
      recipientsSummary.setValueByOwner(text("encrypBackMany.warningMissingRecipients"));
    }

    isDisableControls =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isDisableControls");
    isProgressVisible =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(false), "isProgressVisible");
    progressValue = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(0), "progressValue");
    progressNote = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "progressNote");
  }

  private boolean discoverIsHasMissingRecipients() {
    log.debug("Discovering missing recipients");
    Set<String> allRecipients = enumerateAllRecipientsIds();
    List<Key> keys = keyRingService.findMatchingKeys(allRecipients);
    boolean result = allRecipients.size() > keys.size();
    log.debug("Discovering missing recipients finished. HasMissingRecipients = " + result);
    return result;
  }

  private Set<String> enumerateAllRecipientsIds() {
    Set<String> allRecipients = new HashSet<>();
    for (String file : initParams) {
      EncryptionDialogParameters params =
          encryptionParamsStorage.findParamsBasedOnSourceFile(file, false);
      if (params == null) {
        // we'll deal with this problem later when will try to decrypt
        continue;
      }
      mapFileToEncryptionParams.put(file, params);
      allRecipients.addAll(params.getRecipientsKeysIds());
    }
    return allRecipients;
  }

  @SuppressWarnings("serial")
  protected final Action actionDoOperation =
      new LocalizedActionEx("action.encrypt", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          actionDoOperation.setEnabled(false);
          isDisableControls.setValueByOwner(true);
          encryptionThread.start();
        }
      };

  @SuppressWarnings("serial")
  protected final Action actionCancel =
      new LocalizedActionEx("action.cancel", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          if (encryptionThread.isAlive()) {
            encryptionThread.interrupt();
          } else {
            host.handleClose();
          }
        }
      };

  public static class BatchEncryptionResult implements Serializable {
    private static final long serialVersionUID = -1020511844906379809L;

    public Map<String, Throwable> errors = new HashMap<>();
    public Map<String, Throwable> warnings = new HashMap<>();
    public Multimap<EncryptBackResult, String> categories = ArrayListMultimap.create();
  }

  private Thread encryptionThread =
      new Thread("BkgOpEncryptBackAll") {
        private int totalFiles = 0;
        private int filesProcessed = 0;

        @Override
        public void run() {
          BatchEncryptionResult ret = null;
          try {
            ret = encryptFiles();
          } catch (Throwable t) {
            log.error("Encrypt all failed", t);
            if (ret == null) {
              ret = new BatchEncryptionResult();
            }
            ret.errors.put("OPERATION", t);
          } finally {
            isDisableControls.setValueByOwner(false);
            if (ret == null) {
              ret = new BatchEncryptionResult();
            }

            // summary
            String msg = buildSummaryMessage(ret);
            int severity =
                ret.errors.size() + ret.warnings.size() == 0
                    ? JOptionPane.INFORMATION_MESSAGE
                    : JOptionPane.WARNING_MESSAGE;
            UiUtils.messageBox(
                UiUtils.actionEvent(findRegisteredWindowIfAny(), "BkgOpEncryptBackAll"),
                msg,
                Messages.get("encrypBackMany.action"),
                severity);

            // close window
            host.handleClose();
          }
        }

        private ProgressHandler progressHandler =
            new ProgressHandler() {
              @Override
              public void onProgressUpdated(Progress progress) {
                if (Thread.interrupted()) {
                  progress.requestCancellation();
                }

                if (!isProgressVisible.getValue()) {
                  progressNote.setValueByOwner("");
                  progressValue.setValueByOwner(0);
                  isProgressVisible.setValueByOwner(true);
                } else {
                  progressNote.setValueByOwner(
                      ""
                          + filesProcessed
                          + "/"
                          + totalFiles
                          + ": "
                          + text(progress.getStepCode(), progress.getStepArgs()));
                  progressValue.setValueByOwner(
                      progress.getPercentage() == null ? 0 : progress.getPercentage());
                }

                if (progress.isCompleted()) {
                  isProgressVisible.setValueByOwner(false);
                }
              }
            };

        private BatchEncryptionResult encryptFiles() {
          BatchEncryptionResult ret = new BatchEncryptionResult();

          boolean skipIfissingRecipients = !isIgnoreMissingRecipientsWarning.getValue();
          totalFiles = initParams.size();
          usageLogger.write(new EncryptBackAllUsage(totalFiles));
          for (String decryptedFile : initParams) {
            filesProcessed++;
            // Main operation
            File file = new File(decryptedFile);
            EncryptBackResult oneResult = null;
            try {
              oneResult = encryptBackOne(skipIfissingRecipients, decryptedFile, file, ret);
              ret.categories.put(oneResult, decryptedFile);
            } catch (UserRequestedCancellationException urc) {
              return ret;
            }

            // Post actions
            boolean fileClearedForDeletion =
                oneResult == EncryptBackResult.Encrypted
                    || oneResult == EncryptBackResult.TargetNotChanged;
            if (isDeleteSourceAfter.getValue() && fileClearedForDeletion) {
              try {
                Preconditions.checkState(
                    !file.exists() || file.delete(), "Failed to delete file: ", decryptedFile);
              } catch (Throwable t) {
                ret.warnings.put(decryptedFile, t);
              }
            }
          }
          usageLogger.write(new EncryptBackAllCompletedUsage(ret));
          return ret;
        }

        private EncryptBackResult encryptBackOne(
            boolean isShouldSkipIfissingRecipients,
            String decryptedFile,
            File file,
            BatchEncryptionResult ret)
            throws UserRequestedCancellationException {
          try {
            Preconditions.checkState(file.exists(), text("error.fileNotFound"));
            EncryptionDialogParameters encryptionParams =
                mapFileToEncryptionParams.get(decryptedFile);
            usageLogger.write(new EncryptBackAllIterationUsage(encryptionParams));

            // Check recipients
            Collection<Key> recipients =
                keyRingService.findMatchingKeys(
                    new HashSet<>(encryptionParams.getRecipientsKeysIds()));
            Preconditions.checkState(recipients.size() > 0, text("error.noRecipientsFound"));

            // Check if changed
            DecryptedFile decryptedFileDto =
                monitoringDecryptedFilesService.findByDecryptedFile(decryptedFile);
            boolean targetFileExists = new File(encryptionParams.getTargetFile()).exists();
            boolean proceedOnlyIfChanged = Boolean.TRUE.equals(isEncryptOnlyChanged.getValue());
            boolean hasFingerprintOfDecrypted =
                decryptedFileDto != null && decryptedFileDto.getDecryptedFileFingerprint() != null;
            // TBD: We could just compare size first and only if it matches go through
            // expensive checksum calculation
            if (targetFileExists && proceedOnlyIfChanged && hasFingerprintOfDecrypted) {
              Fingerprint decryptedFileFingerprint = calculateFingerprintSync(decryptedFile);
              if (decryptedFileDto.getDecryptedFileFingerprint().equals(decryptedFileFingerprint)) {
                return EncryptBackResult.TargetNotChanged;
              }

              // now let's check source file to make sure no concurrent changes
              if (decryptedFileDto.getEncryptedFileFingerprint() != null) {
                Fingerprint encryptedFileFingerprint =
                    calculateFingerprintSync(encryptionParams.getTargetFile());
                if (!decryptedFileDto
                    .getEncryptedFileFingerprint()
                    .equals(encryptedFileFingerprint)) {
                  ret.warnings.put(
                      decryptedFile, new GenericException("error.concurrentChangeOfEncryptedFile"));
                  return EncryptBackResult.ConcurrentChangeDetected;
                }
              }
            }

            boolean isMissingRecipients =
                recipients.size() < encryptionParams.getRecipientsKeysIds().size();
            if (isMissingRecipients && isShouldSkipIfissingRecipients) {
              ret.warnings.put(decryptedFile, new GenericException("error.someKeysAreMissing"));
              return EncryptBackResult.MissingRecipients;
            }

            // Actually encrypt
            ChecksumCalcInputStreamSupervisor inputStreamSupervisor =
                new ChecksumCalcInputStreamSupervisorImpl(messageDigestFactory);
            ChecksumCalcOutputStreamSupervisor outputStreamSupervisor =
                new ChecksumCalcOutputStreamSupervisorImpl(messageDigestFactory);

            FileUtilsEx.baitAndSwitch(
                encryptionParams.getTargetFile(),
                x ->
                    encryptionService.encrypt(
                        decryptedFile,
                        x,
                        recipients,
                        progressHandler,
                        inputStreamSupervisor,
                        outputStreamSupervisor));

            // Update fingerprints in relevant DecriptedFile dto
            updateBaselineFingerprintsIfApplicable(
                decryptedFileDto,
                decryptedFile,
                encryptionParams,
                inputStreamSupervisor.getFingerprint(),
                outputStreamSupervisor.getFingerprint());

            return EncryptBackResult.Encrypted;
          } catch (Throwable t) {
            log.error("Encryption failed: " + decryptedFile, t);

            Throwables.throwIfInstanceOf(t, UserRequestedCancellationException.class);
            ret.errors.put(decryptedFile, t);
            return EncryptBackResult.Exception;
          }
        }

        private void updateBaselineFingerprintsIfApplicable(
            DecryptedFile decryptedFileDto,
            String decryptedFile,
            EncryptionDialogParameters encryptionParams,
            Fingerprint sourceFingerprint,
            Fingerprint targetFingerprint) {
          if (decryptedFileDto == null) {
            return;
          }

          DecryptedFile newDecryptedFile = DeepCopy.copy(decryptedFileDto);
          newDecryptedFile.setDecryptedFileFingerprint(sourceFingerprint);
          newDecryptedFile.setEncryptedFileFingerprint(targetFingerprint);
          monitoringDecryptedFilesService.addOrUpdate(newDecryptedFile);
        }

        private Fingerprint calculateFingerprintSync(String filePathname) throws Exception {
          return new ChecksumCalculationTask(
                  filePathname, messageDigestFactory.createNew(), progressHandler)
              .call();
        }

        private String buildSummaryMessage(BatchEncryptionResult ret) {
          StringBuilder sb = new StringBuilder();
          if (ret.errors.size() + ret.warnings.size() == 0) {
            sb.append(text("phrase.operationCompletedWithSuccess"));
          } else {
            sb.append(text("phrase.operationCompletedWithErrors"));
          }
          sb.append("\n");

          boolean first = true;
          for (EncryptBackResult resultType : ret.categories.keySet()) {
            if (!first) {
              sb.append("\r\n");
            }
            first = false;

            sb.append(" - ");
            sb.append(text("encrBackAll." + resultType.name()));
            sb.append(": ");
            sb.append(ret.categories.get(resultType).size());
          }
          sb.append("\n");

          if (ret.warnings.size() > 0) {
            sb.append("\n");
            sb.append(text("term.warnings").toUpperCase());
            sb.append("\n");
            listExceptions(ret.warnings, sb);
          }

          if (ret.errors.size() > 0) {
            sb.append("\n");
            sb.append(text("term.errors").toUpperCase());
            sb.append("\n");
            listExceptions(ret.errors, sb);
          }

          return sb.toString();
        }

        private void listExceptions(Map<String, Throwable> exceptions, StringBuilder sb) {
          for (Entry<String, Throwable> exc : exceptions.entrySet()) {
            sb.append("\n");
            sb.append(exc.getKey());
            sb.append("\n");
            sb.append(ConsoleExceptionUtils.getAllMessages(exc.getValue()));
            sb.append("\n");
          }
        }
      };

  public ModelPropertyAccessor<Boolean> getIsDeleteSourceAfter() {
    return isDeleteSourceAfter.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<Boolean> getIsHasMissingRecipients() {
    return isHasMissingRecipients.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<Boolean> getIsIgnoreMissingRecipientsWarning() {
    return isIgnoreMissingRecipientsWarning.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getSourceFilesSummary() {
    return sourceFilesSummary.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getRecipientsSummary() {
    return recipientsSummary.getModelPropertyAccessor();
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

  public ModelPropertyAccessor<Boolean> getIsEncryptOnlyChanged() {
    return isEncryptOnlyChanged.getModelPropertyAccessor();
  }
}
