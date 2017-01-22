package org.pgpvault.gui.encryption.api.dto;

import java.io.Serializable;

/**
 * This class is mostly used to render key info for the user
 * 
 * @author sergeyk
 *
 */
public class KeyInfo implements Serializable {
	private static final long serialVersionUID = 7017129505577264717L;

	/**
	 * User name + email
	 */
	private String user;
	private String keyId;

	private KeyTypeEnum keyType;

	/**
	 * Algorythm info + size
	 */
	private String keyAlgorithm;
	private java.sql.Date createdOn;
	private java.sql.Date expiresAt;

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public KeyTypeEnum getKeyType() {
		return keyType;
	}

	public void setKeyType(KeyTypeEnum keyType) {
		this.keyType = keyType;
	}

	public String getKeyAlgorithm() {
		return keyAlgorithm;
	}

	public void setKeyAlgorithm(String keyAlgorithm) {
		this.keyAlgorithm = keyAlgorithm;
	}

	public java.sql.Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(java.sql.Date createdOn) {
		this.createdOn = createdOn;
	}

	public java.sql.Date getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(java.sql.Date expiresAt) {
		this.expiresAt = expiresAt;
	}
}
