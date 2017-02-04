package org.pgptool.gui.decryptedlist.api;

import java.io.Serializable;

public class DecryptedFile implements Serializable {
	private static final long serialVersionUID = -6339203835731870415L;

	private String encryptedFile;
	private String decryptedFile;

	// private List<String> recipientsKeysIds;

	public DecryptedFile() {
	}

	public DecryptedFile(String encryptedFile, String decryptedFile) {
		this.encryptedFile = encryptedFile;
		this.decryptedFile = decryptedFile;
	}

	public String getEncryptedFile() {
		return encryptedFile;
	}

	public void setEncryptedFile(String encryptedFile) {
		this.encryptedFile = encryptedFile;
	}

	public String getDecryptedFile() {
		return decryptedFile;
	}

	public void setDecryptedFile(String decryptedFile) {
		this.decryptedFile = decryptedFile;
	}
}
