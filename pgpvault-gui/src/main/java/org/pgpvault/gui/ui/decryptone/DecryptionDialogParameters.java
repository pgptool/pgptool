package org.pgpvault.gui.ui.decryptone;

import java.io.Serializable;

/**
 * This class is used to reflect dialog parameters. It's used to help suggest
 * user with processing parameters based on input parameter(s)
 * 
 * @author sergeyk
 *
 */
public class DecryptionDialogParameters implements Serializable {
	private static final long serialVersionUID = 1090348970039260219L;

	private String sourceFile;

	private boolean useSameFolder;
	private String targetFile;

	private String decryptionKeyId;

	private boolean isDeleteSourceFile;
	private boolean isOpenTargetFolder;
	private boolean isOpenAssociatedApplication;

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((decryptionKeyId == null) ? 0 : decryptionKeyId.hashCode());
		result = prime * result + (isDeleteSourceFile ? 1231 : 1237);
		result = prime * result + (isOpenAssociatedApplication ? 1231 : 1237);
		result = prime * result + (isOpenTargetFolder ? 1231 : 1237);
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		result = prime * result + ((targetFile == null) ? 0 : targetFile.hashCode());
		result = prime * result + (useSameFolder ? 1231 : 1237);
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
		return true;
	}
}
