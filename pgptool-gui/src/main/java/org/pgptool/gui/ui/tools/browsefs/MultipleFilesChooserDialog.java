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
package org.pgptool.gui.ui.tools.browsefs;

import java.awt.Component;
import java.io.File;

import javax.swing.JFileChooser;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.springframework.util.StringUtils;

public class MultipleFilesChooserDialog {
	private static Logger log = Logger.getLogger(MultipleFilesChooserDialog.class);

	private Component optionalParent;
	private ConfigPairs configPairs;
	private String configPairNameToRemember;

	public MultipleFilesChooserDialog(Component optionalParent, ConfigPairs configPairs,
			String configPairNameToRemember) {
		this.optionalParent = optionalParent;
		this.configPairs = configPairs;
		this.configPairNameToRemember = configPairNameToRemember;
	}

	/**
	 * Blocking call to browse for files
	 * 
	 * @return null if nothing was chosen, or array of files chosen otherwise
	 */
	public File[] askUserForMultipleFiles() {
		JFileChooser ofd = buildFileChooserDialog();

		int result = ofd.showOpenDialog(optionalParent);
		if (result != JFileChooser.APPROVE_OPTION) {
			handleFilesWereChosen(null);
			return null;
		}
		File[] retFile = ofd.getSelectedFiles();
		if (retFile == null || retFile.length == 0) {
			handleFilesWereChosen(null);
			return null;
		}

		handleFilesWereChosen(retFile);
		configPairs.put(configPairNameToRemember,
				FilenameUtils.getFullPathNoEndSeparator(retFile[0].getAbsolutePath()));
		return retFile;
	}

	/**
	 * Subclass can do post-processing if needed
	 * 
	 * @param retFile
	 *            might be null if no files were chosen
	 */
	protected void handleFilesWereChosen(File[] retFile) {
	}

	private JFileChooser buildFileChooserDialog() {
		JFileChooser ofd = new JFileChooser();
		ofd.setFileSelectionMode(JFileChooser.FILES_ONLY);
		ofd.setAcceptAllFileFilterUsed(true);
		ofd.setMultiSelectionEnabled(true);
		ofd.setDialogTitle(Messages.get("action.chooseExistingFile"));
		ofd.setApproveButtonText(Messages.get("action.choose"));
		suggestInitialDirectory(ofd);
		doFileChooserPostConstruct(ofd);
		return ofd;
	}

	protected void doFileChooserPostConstruct(JFileChooser ofd) {
		// NOTE: It's meant to be overridden. Subclass can augment chooser
		// if needed
	}

	protected void suggestInitialDirectory(JFileChooser ofd) {
		try {
			String lastChosenDestination = configPairs.find(configPairNameToRemember, null);
			String pathname = StringUtils.hasText(lastChosenDestination) ? lastChosenDestination
					: SystemUtils.getUserHome().getAbsolutePath();
			ofd.setCurrentDirectory(new File(pathname));
		} catch (Throwable t) {
			log.warn("Failed to set suggested location by key " + configPairNameToRemember, t);
		}
	}
}
