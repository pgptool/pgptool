package org.pgptool.gui.ui.keyslist;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;

import javax.annotation.Resource;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.hintsforusage.hints.PrivateKeyBackupHint.PrivateKeyExportedEvent;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.FolderChooserDialog;
import org.pgptool.gui.ui.tools.browsefs.SaveFileChooserDialog;
import org.pgptool.gui.ui.tools.browsefs.ValueAdapterPersistentPropertyImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class KeysExporterUiImpl implements KeysExporterUi {
	private static Logger log = Logger.getLogger(KeysExporterUiImpl.class);

	@Autowired
	@Resource(name = "keyFilesOperations")
	private KeyFilesOperations<KeyData> keyFilesOperations;
	@Autowired
	private EventBus eventBus;
	@Autowired
	private ConfigPairs appProps;

	private FolderChooserDialog folderChooserDialog;

	@Override
	public void exportPublicKey(Key<KeyData> key, Window parentWindow) {
		String targetFile = buildPublicKeyTargetChooser(key, parentWindow).askUserForFile();
		if (targetFile == null) {
			return;
		}

		try {
			keyFilesOperations.exportPublicKey(key, targetFile);
		} catch (Throwable t) {
			log.error("Failed to export key " + key, t);
			EntryPoint.reportExceptionToUser("error.failedToExportPublicKey", t, key.toString());
			return;
		}

		browseForFolder(FilenameUtils.getFullPath(targetFile));
	}

	public SaveFileChooserDialog buildPublicKeyTargetChooser(Key<KeyData> key, Window parentWindow) {
		return new SaveFileChooserDialog(parentWindow, "action.exportPublicKey", "action.export", appProps,
				"ExportKeyDialog") {
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
				File suggestedFileName = suggestFileNameForKey(key, ofd.getCurrentDirectory().getAbsolutePath(), null,
						false, false);
				ofd.setSelectedFile(suggestedFileName);
			}
		};
	}

	@Override
	public void exportPrivateKey(Key<KeyData> key, Window parentWindow) {
		String targetFile = buildPrivateKeyTargetChooser(key, parentWindow).askUserForFile();
		if (targetFile == null) {
			return;
		}

		try {
			keyFilesOperations.exportPrivateKey(key, targetFile);
			eventBus.post(EntityChangedEvent.added(new PrivateKeyExportedEvent(key)));
		} catch (Throwable t) {
			log.error("Failed to export private key " + key, t);
			EntryPoint.reportExceptionToUser("error.failedToExportPrivateKey", t, key.toString());
			return;
		}

		UiUtils.messageBox(parentWindow, text("keys.privateKey.exportWarning"), text("term.attention"),
				JOptionPane.WARNING_MESSAGE);
		browseForFolder(FilenameUtils.getFullPath(targetFile));
	}

	public SaveFileChooserDialog buildPrivateKeyTargetChooser(final Key<KeyData> key, Window parentWindow) {
		return new SaveFileChooserDialog(parentWindow, "action.exportPrivateKey", "action.export", appProps,
				"ExportKeyDialog") {
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

				File suggestedFileName = suggestFileNameForKey(key, ofd.getCurrentDirectory().getAbsolutePath(),
						" - private", false, false);
				ofd.setSelectedFile(suggestedFileName);
			}
		};
	}

	private File suggestFileNameForKey(Key<KeyData> key, String basePathNoSlash, String optionalSuffix,
			boolean isAddExtension, boolean isMitigateOverwrite) {
		String userName = key.getKeyInfo().buildUserNameOnly();
		String fileName = basePathNoSlash + File.separator + userName;
		String fileNameWithoutExt = fileName;
		if (optionalSuffix != null) {
			fileName += optionalSuffix;
		}
		if (isAddExtension) {
			fileName += ".asc";
		}
		if (isMitigateOverwrite) {
			fileName = addKeyIdIfFileAlreadyExists(key, isAddExtension, fileName, fileNameWithoutExt);
		}
		return new File(fileName);
	}

	private String addKeyIdIfFileAlreadyExists(Key<KeyData> key, boolean isAddExtension, String fileName,
			String fileNameWithoutExt) {
		if (!new File(fileName).exists()) {
			return fileName;
		}

		fileName = fileNameWithoutExt + "-" + key.getKeyInfo().getKeyId();
		if (isAddExtension) {
			fileName += ".asc";
		}
		return fileName;
	}

	@Override
	public void exportPublicKeys(ArrayList<Key<KeyData>> keys, Window parentWindow) {
		String newFolder = getFolderChooserDialog().askUserForFolder(parentWindow);
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
				File targetFile = suggestFileNameForKey(key, newFolder, null, true, true);
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
					appProps, "KeysListPm.exportedKeysLocation", null);
			folderChooserDialog = new FolderChooserDialog(text("keys.chooseFolderForKeysExport"), exportedKeysLocation);
		}
		return folderChooserDialog;
	}

	private void browseForFolder(String targetFileName) {
		try {
			Desktop.getDesktop().browse(new File(targetFileName).toURI());
		} catch (Throwable t) {
			log.warn("Failed to open folder for exported key", t);
		}
	}

}
