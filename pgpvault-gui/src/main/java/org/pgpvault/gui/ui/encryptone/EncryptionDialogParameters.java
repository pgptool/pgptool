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
