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

import java.io.Serializable;
import java.util.ArrayList;

import org.pgptool.gui.encryptionparams.api.EncryptionParamsStorage;

/**
 * This class is used to reflect dialog parameters. It's used to help suggest
 * user with processing parameters based on input parameter(s)
 * 
 * @author Sergey Karpushin
 * 
 *         TODO: Move this DTO to appropriate package near
 *         {@link EncryptionParamsStorage} BUT !!!!!!! This must be performed as
 *         a part of upgrade procedure to avoid screwing up peoples current
 *         settings!!
 *
 */
public class EncryptionDialogParameters implements Serializable {
	private static final long serialVersionUID = -8793919231868733870L;

	private String sourceFile;

	private boolean useSameFolder;
	private String targetFile;
	private ArrayList<String> recipientsKeysIds;
	private boolean isDeleteSourceFile;
	private boolean isOpenTargetFolder;

	/**
	 * If true it means this instance was created by decrypt dialog to help
	 * suggest parameters on encryption
	 */
	private boolean isPropagatedFromDecrypt;

	public boolean isSameInputAs(EncryptionDialogParameters b) {
		if (b == null || b.getSourceFile() == null || sourceFile == null) {
			return false;
		}
		return sourceFile.equals(b.getSourceFile());
	}

	public String getSourceFile() {
		return sourceFile;
	}

	public void setSourceFile(String inputFile) {
		this.sourceFile = inputFile;
	}

	public boolean isUseSameFolder() {
		return useSameFolder;
	}

	public void setUseSameFolder(boolean useSameFolder) {
		this.useSameFolder = useSameFolder;
	}

	public String getTargetFile() {
		return targetFile;
	}

	public void setTargetFile(String targetFile) {
		this.targetFile = targetFile;
	}

	public ArrayList<String> getRecipientsKeysIds() {
		return recipientsKeysIds;
	}

	public void setRecipientsKeysIds(ArrayList<String> recipientsKeysIds) {
		this.recipientsKeysIds = recipientsKeysIds;
	}

	public boolean isDeleteSourceFile() {
		return isDeleteSourceFile;
	}

	public void setDeleteSourceFile(boolean isDeleteSourceFile) {
		this.isDeleteSourceFile = isDeleteSourceFile;
	}

	public boolean isOpenTargetFolder() {
		return isOpenTargetFolder;
	}

	public void setOpenTargetFolder(boolean isOpenTargetFolder) {
		this.isOpenTargetFolder = isOpenTargetFolder;
	}

	public boolean isPropagatedFromDecrypt() {
		return isPropagatedFromDecrypt;
	}

	public void setPropagatedFromDecrypt(boolean isPropagatedFromDecrypt) {
		this.isPropagatedFromDecrypt = isPropagatedFromDecrypt;
	}
}
