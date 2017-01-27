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
	private boolean useTempFolder;
	private String targetFile; // means user excplicitly provided it

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

	public boolean isUseTempFolder() {
		return useTempFolder;
	}

	public void setUseTempFolder(boolean useTempFolder) {
		this.useTempFolder = useTempFolder;
	}
}
