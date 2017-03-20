package org.pgptool.gui.ui.getkeypassword;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;

public class PasswordDeterminedForKey<TKeyData extends KeyData> {
	private String decryptionKeyId;
	private Key<TKeyData> matchedKey;
	private String password;

	public PasswordDeterminedForKey(String decryptionKeyId, Key<TKeyData> matchedKey, String password) {
		super();
		this.decryptionKeyId = decryptionKeyId;
		this.matchedKey = matchedKey;
		this.password = password;
	}

	public Key<TKeyData> getMatchedKey() {
		return matchedKey;
	}

	public void setMatchedKey(Key<TKeyData> key) {
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
