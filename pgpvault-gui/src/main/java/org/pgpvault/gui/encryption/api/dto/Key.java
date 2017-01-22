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

	@SuppressWarnings("rawtypes")
	public static boolean isSameKeyId(Key o1, Key o2) {
		if (o1 == null || o2 == null) {
			return false;
		}
		if (o1.getKeyInfo() == null || o2.getKeyInfo() == null) {
			return false;
		}
		if (o1.getKeyInfo().getKeyId() == null || o2.getKeyInfo().getKeyId() == null) {
			return false;
		}

		return o1.getKeyInfo().getKeyId().equals(o2.getKeyInfo().getKeyId());
	}
}
