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
package org.pgptool.gui.ui.decryptone;

import java.io.Serializable;

import org.summerb.approaches.jdbccrud.common.DtoBase;

/**
 * This class is used to reflect dialog parameters. It's used to help suggest
 * user with processing parameters based on input parameter(s)
 * 
 * @author Sergey Karpushin
 *
 */
public class DecryptionDialogParameters implements Serializable, DtoBase {
	private static final long serialVersionUID = 1090348970039260219L;

	private String sourceFile;

	private boolean useSameFolder;
	private boolean useTempFolder;
	private String targetFile; // means user explicitly provided it

	private String decryptionKeyId;

	private boolean isDeleteSourceFile;
	private boolean isOpenTargetFolder;
	private boolean isOpenAssociatedApplication;

	private long createdAt;

	public String getSourceFile() {
		return sourceFile;
	}

	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
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

	public String getDecryptionKeyId() {
		return decryptionKeyId;
	}

	public void setDecryptionKeyId(String decryptionKeyId) {
		this.decryptionKeyId = decryptionKeyId;
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

	public boolean isOpenAssociatedApplication() {
		return isOpenAssociatedApplication;
	}

	public void setOpenAssociatedApplication(boolean isOpenAssociatedApplication) {
		this.isOpenAssociatedApplication = isOpenAssociatedApplication;
	}

	public boolean isUseTempFolder() {
		return useTempFolder;
	}

	public void setUseTempFolder(boolean useTempFolder) {
		this.useTempFolder = useTempFolder;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(long createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (createdAt ^ (createdAt >>> 32));
		result = prime * result + ((decryptionKeyId == null) ? 0 : decryptionKeyId.hashCode());
		result = prime * result + (isDeleteSourceFile ? 1231 : 1237);
		result = prime * result + (isOpenAssociatedApplication ? 1231 : 1237);
		result = prime * result + (isOpenTargetFolder ? 1231 : 1237);
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		result = prime * result + ((targetFile == null) ? 0 : targetFile.hashCode());
		result = prime * result + (useSameFolder ? 1231 : 1237);
		result = prime * result + (useTempFolder ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DecryptionDialogParameters other = (DecryptionDialogParameters) obj;
		if (createdAt != other.createdAt) {
			return false;
		}
		if (decryptionKeyId == null) {
			if (other.decryptionKeyId != null) {
				return false;
			}
		} else if (!decryptionKeyId.equals(other.decryptionKeyId)) {
			return false;
		}
		if (isDeleteSourceFile != other.isDeleteSourceFile) {
			return false;
		}
		if (isOpenAssociatedApplication != other.isOpenAssociatedApplication) {
			return false;
		}
		if (isOpenTargetFolder != other.isOpenTargetFolder) {
			return false;
		}
		if (sourceFile == null) {
			if (other.sourceFile != null) {
				return false;
			}
		} else if (!sourceFile.equals(other.sourceFile)) {
			return false;
		}
		if (targetFile == null) {
			if (other.targetFile != null) {
				return false;
			}
		} else if (!targetFile.equals(other.targetFile)) {
			return false;
		}
		if (useSameFolder != other.useSameFolder) {
			return false;
		}
		if (useTempFolder != other.useTempFolder) {
			return false;
		}
		return true;
	}
}
