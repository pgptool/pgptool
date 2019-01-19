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
package org.pgptool.gui.encryptionparams.impl;

import org.apache.commons.io.FilenameUtils;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryptionparams.api.EncryptionParamsStorage;
import org.pgptool.gui.ui.encryptone.EncryptionDialogParameters;
import org.springframework.beans.factory.annotation.Autowired;

public class EncryptionParamsStorageImpl implements EncryptionParamsStorage {
	public static final String CONFIG_PAIR_BASE = "Encrypt:";

	@Autowired
	private ConfigPairs encryptionParams;

	@Override
	public void persistDialogParametersForCurrentInputs(EncryptionDialogParameters dialogParameters,
			boolean updateFolderSettings) {
		encryptionParams.put(dialogParameters.getSourceFile(), dialogParameters);
		if (updateFolderSettings) {
			encryptionParams.put(FilenameUtils.getFullPathNoEndSeparator(dialogParameters.getSourceFile()),
					dialogParameters);
		}
	}

	@Override
	public EncryptionDialogParameters findParamsBasedOnSourceFile(String sourceFile,
			boolean fallBackToFolderSettingsIfAny) {
		EncryptionDialogParameters params = encryptionParams.find(sourceFile, null);
		if (fallBackToFolderSettingsIfAny && params == null) {
			params = encryptionParams.find(FilenameUtils.getFullPathNoEndSeparator(sourceFile), null);
		}
		return params;
	}

}
