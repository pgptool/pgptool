package org.pgptool.gui.ui.tempfolderfordecrypted;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.springframework.beans.factory.annotation.Autowired;

import ru.skarpushin.swingpm.base.PresentationModelBase;

/**
 * This simple presenter will provide user with ability to select other folder
 * to store temporarily decrypted files
 * 
 * @author Sergey Karpushin
 *
 */
public class TempFolderChooserPm extends PresentationModelBase {
	private static Logger log = Logger.getLogger(TempFolderChooserPm.class);

	@Autowired
	private DecryptedTempFolder decryptedTempFolder;

	public void present(Component parent) {
		String newFolder = askuserForFolder(parent);
		if (newFolder == null) {
			return;
		}
		try {
			decryptedTempFolder.setTempFolderBasePath(newFolder);
			EntryPoint.showMessageBox(parent, text("phrase.settingsChangedConfirmFolder", newFolder),
					text("term.success"), JOptionPane.INFORMATION_MESSAGE);
		} catch (Throwable t) {
			log.error("Failed to save settigns for folder to use for temp decrypted files", t);
			EntryPoint.reportExceptionToUser("error.failedToSetNewTempFolder", t);
		}
	}

	private String askuserForFolder(Component parent) {
		JFileChooser ofd = new JFileChooser();
		ofd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		ofd.setAcceptAllFileFilterUsed(true);
		ofd.setMultiSelectionEnabled(false);
		ofd.setDialogTitle(Messages.get("term.changeTempFolderForDecrypted"));
		ofd.setApproveButtonText(Messages.get("action.choose"));
		ofd.setCurrentDirectory(new File(decryptedTempFolder.getTempFolderBasePath()));
		ofd.setSelectedFile(new File(decryptedTempFolder.getTempFolderBasePath()));

		int result = ofd.showOpenDialog(parent);
		if (result != JFileChooser.APPROVE_OPTION) {
			return null;
		}

		File ret = ofd.getSelectedFile();
		if (ret == null) {
			return null;
		}

		return ret.getAbsolutePath();
	}
}
