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

import org.springframework.util.StringUtils;

/**
 * This class is mostly used to render key info for the user
 * 
 * @author Sergey Karpushin
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

	/**
	 * Returns user name without email or any other special symbols
	 * 
	 * NOTE: This method doesn't seem to belong to this class because it feels like
	 * an impl-specific thing
	 * 
	 * NOTE 2: Name of this is not started with "get" to avoid serialization
	 * confusion
	 * 
	 * @return user name that can be used as a file name
	 */
	public String buildUserNameOnly() {
		String name = getUser();
		if (!StringUtils.hasText(name)) {
			return "";
		}

		// get id of email
		int pos = name.indexOf("<");
		if (pos > 0) {
			name = name.substring(0, pos);
		}

		// get rid of special symbols
		name = name.replaceAll("[.,!@#$%^&*()`'\"\\[\\]\\\\]", " ");

		// remove double spaces
		name = name.replace("  ", " ");

		return name.trim();
	}
}
