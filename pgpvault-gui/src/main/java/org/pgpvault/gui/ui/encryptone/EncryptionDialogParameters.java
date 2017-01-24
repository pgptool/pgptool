package org.pgpvault.gui.ui.encryptone;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class is used to reflect dialog parameters. It's used to help suggest
 * user with processing parameters based on input parameter(s)
 * 
 * @author sergeyk
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		result = prime * result + (isDeleteSourceFile ? 1231 : 1237);
		result = prime * result + (isOpenTargetFolder ? 1231 : 1237);
		result = prime * result + ((recipientsKeysIds == null) ? 0 : recipientsKeysIds.hashCode());
		result = prime * result + ((targetFile == null) ? 0 : targetFile.hashCode());
		result = prime * result + (useSameFolder ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EncryptionDialogParameters other = (EncryptionDialogParameters) obj;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
			return false;
		if (isDeleteSourceFile != other.isDeleteSourceFile)
			return false;
		if (isOpenTargetFolder != other.isOpenTargetFolder)
			return false;
		if (recipientsKeysIds == null) {
			if (other.recipientsKeysIds != null)
				return false;
		} else if (!recipientsKeysIds.equals(other.recipientsKeysIds))
			return false;
		if (targetFile == null) {
			if (other.targetFile != null)
				return false;
		} else if (!targetFile.equals(other.targetFile))
			return false;
		if (useSameFolder != other.useSameFolder)
			return false;
		return true;
	}
}
