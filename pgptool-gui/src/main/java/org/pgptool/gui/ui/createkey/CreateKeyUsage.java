package org.pgptool.gui.ui.createkey;

import java.io.Serializable;

import org.pgptool.gui.encryption.api.dto.CreateKeyParams;

public class CreateKeyUsage implements Serializable {
	private static final long serialVersionUID = -6652489944057680318L;
	private String userName;
	private String email;

	public CreateKeyUsage() {
	}

	public CreateKeyUsage(CreateKeyParams createKeyParams) {
		userName = createKeyParams.getFullName();
		email = createKeyParams.getEmail();
		// NOTE: We're not saving passphrase intentionally!!!
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
