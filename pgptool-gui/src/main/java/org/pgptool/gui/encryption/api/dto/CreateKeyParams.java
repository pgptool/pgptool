package org.pgptool.gui.encryption.api.dto;

import java.io.Serializable;

public class CreateKeyParams implements Serializable {
	private static final long serialVersionUID = 4508794770144261040L;

	public static final String FN_FULL_NAME = "fullName";
	public static final String FN_EMAIL = "email";
	public static final String FN_PASSPHRASE = "passphrase";
	public static final String FN_PASSPHRASE_AGAIN = "passphraseAgain";

	private String fullName;
	private String email;
	private String passphrase;
	private String passphraseAgain;

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

	public String getPassphraseAgain() {
		return passphraseAgain;
	}

	public void setPassphraseAgain(String passphraseAgain) {
		this.passphraseAgain = passphraseAgain;
	}
}
