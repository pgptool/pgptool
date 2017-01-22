package org.pgpvault.gui.encryption.api.dto;

import java.io.Serializable;

public class Key<TKeyData extends KeyData> implements Serializable {
	private static final long serialVersionUID = 1614562515516152578L;

	/**
	 * This field contains some parts of parsed data. It's intended for
	 * read-only use, do not change it manually
	 */
	private KeyInfo keyInfo;
	private TKeyData keyData;

	public KeyInfo getKeyInfo() {
		return keyInfo;
	}

	/**
	 * @deprecated for IO purposes only, do not modify manually
	 */
	@Deprecated
	public void setKeyInfo(KeyInfo keyInfo) {
		this.keyInfo = keyInfo;
	}

	public TKeyData getKeyData() {
		return keyData;
	}

	public void setKeyData(TKeyData keyData) {
		this.keyData = keyData;
	}
}
