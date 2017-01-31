package org.pgpvault.gui.ui.tools;

import java.awt.Window;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
import org.springframework.util.StringUtils;

public class SaveFileChooserDialog {
	private static Logger log = Logger.getLogger(SaveFileChooserDialog.class);

	private String dialogTitleCode;
	private String approvalButtonTextCode;
	private Window parentWindow;
	private ConfigPairs configPairs;
	private String configId;

	public SaveFileChooserDialog(Window parentWindow, String dialogTitleCode, String approvalButtonTextCode) {
		this.parentWindow = parentWindow;
		this.dialogTitleCode = dialogTitleCode;
		this.approvalButtonTextCode = approvalButtonTextCode;
	}

	public SaveFileChooserDialog(Window parentWindow, String dialogTitleCode, String approvalButtonTextCode,
			ConfigPairs configPairs, String configId) {
		this(parentWindow, dialogTitleCode, approvalButtonTextCode);
		this.configPairs = configPairs;
		this.configId = configId;
	}

	public String askUserForFile() {
		JFileChooser ofd = new JFileChooser();
		ofd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		ofd.setMultiSelectionEnabled(false);
		ofd.setDialogTitle(Messages.get(dialogTitleCode));
		ofd.setApproveButtonText(Messages.get(approvalButtonTextCode));
		onFileChooserPostConstrct(ofd);
		suggestTarget(ofd);

		int result = ofd.showSaveDialog(parentWindow);
		if (result != JFileChooser.APPROVE_OPTION) {
			return onDialogClosed(null, ofd);
		}
		File retFile = ofd.getSelectedFile();
		if (retFile == null) {
			return onDialogClosed(null, ofd);
		}

		String ret = retFile.getAbsolutePath();
		ret = onDialogClosed(ret, ofd);
		return ret;
	}

	protected void onFileChooserPostConstrct(JFileChooser ofd) {
	}

	protected void suggestTarget(JFileChooser ofd) {
		if (configPairs == null || configId == null) {
			return;
		}

		try {
			String lastChosenDestination = configPairs.find(configId, null);
			String pathname = StringUtils.hasText(lastChosenDestination) ? lastChosenDestination
					: SystemUtils.getUserHome().getAbsolutePath();
			ofd.setCurrentDirectory(new File(pathname));
		} catch (Throwable t) {
			log.warn("Failed to set suggested location for dialog " + dialogTitleCode, t);
		}
	}

	/**
	 * Subclass can do post-processing if needed
	 * 
	 * @param filePathName
	 *            user choice, might be null
	 * @param ofd
	 *            dialog
	 * @return value that will be returned to initial invoker
	 */
	protected String onDialogClosed(String filePathName, JFileChooser ofd) {
		if (filePathName == null) {
			return null;
		}

		if (configPairs != null && configId != null) {
			configPairs.put(configId, FilenameUtils.getFullPathNoEndSeparator(filePathName));
		}

		return enforceExtension(filePathName, ofd);
	}

	private String enforceExtension(String filePathName, JFileChooser ofd) {
		FileFilter fileExtFilter = ofd.getFileFilter();
		if (fileExtFilter == ofd.getAcceptAllFileFilter()) {
			return filePathName;
		}
		FileNameExtensionFilter fileNameExtensionFilter = (FileNameExtensionFilter) fileExtFilter;
		String ext = fileNameExtensionFilter.getExtensions()[0];
		if (!ext.equalsIgnoreCase(FilenameUtils.getExtension(filePathName))) {
			filePathName += "." + ext;
		}
		return filePathName;
	}
}