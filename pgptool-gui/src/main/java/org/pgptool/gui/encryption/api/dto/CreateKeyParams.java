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
