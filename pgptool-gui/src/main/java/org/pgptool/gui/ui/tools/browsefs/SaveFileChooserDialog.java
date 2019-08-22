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
package org.pgptool.gui.ui.tools.browsefs;

import java.awt.Window;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

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
		JFileChooser ofd = prepareFileChooser();

		boolean userWantsToSelectOtherFile = true;
		File retFile = null;
		while (userWantsToSelectOtherFile) {
			int result = ofd.showSaveDialog(parentWindow);
			if (result != JFileChooser.APPROVE_OPTION) {
				return onDialogClosed(null, ofd);
			}
			retFile = ofd.getSelectedFile();
			if (retFile == null) {
				return onDialogClosed(null, ofd);
			}

			File retFileWithExt = new File(enforceExtension(retFile.getAbsolutePath(), ofd));
			if (userWantsToSelectOtherFile = retFileWithExt.exists()) {
				if (UiUtils.confirmRegular("confirm.overWriteExistingFile", new Object[] { retFileWithExt.getAbsolutePath() },
						parentWindow)) {
					userWantsToSelectOtherFile = false;
				}
			}
		}

		Preconditions.checkState(retFile != null, "Useless check but it makes Eclipse NPE checker happier");
		String ret = retFile.getAbsolutePath();
		ret = onDialogClosed(ret, ofd);
		return ret;
	}

	private JFileChooser prepareFileChooser() {
		JFileChooser ofd = new JFileChooser();
		ofd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		ofd.setMultiSelectionEnabled(false);
		ofd.setDialogTitle(Messages.get(dialogTitleCode));
		ofd.setApproveButtonText(Messages.get(approvalButtonTextCode));
		onFileChooserPostConstrct(ofd);
		suggestTarget(ofd);
		return ofd;
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
