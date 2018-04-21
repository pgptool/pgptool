package org.pgptool.gui.ui.getkeypassword;

import org.pgptool.gui.encryption.api.dto.Key;

public class PasswordDeterminedForKey {
	private String decryptionKeyId;
	private Key matchedKey;
	private String password;

	public PasswordDeterminedForKey(String decryptionKeyId, Key matchedKey, String password) {
		super();
		this.decryptionKeyId = decryptionKeyId;
		this.matchedKey = matchedKey;
		this.password = password;
	}

	public Key getMatchedKey() {
		return matchedKey;
	}

	public void setMatchedKey(Key key) {
		this.matchedKey = key;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDecryptionKeyId() {
		return decryptionKeyId;
	}

	public void setDecryptionKeyId(String decryptionKeyId) {
		this.decryptionKeyId = decryptionKeyId;
	}
}
