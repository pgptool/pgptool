/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
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
 *******************************************************************************/
package org.pgptool.gui.encryption.api;

import java.util.Collection;
import java.util.Set;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.summerb.approaches.security.api.exceptions.InvalidPasswordException;

public interface EncryptionService<TKeyData extends KeyData> {
	void encrypt(String sourceFile, String targetFile, Collection<Key<TKeyData>> recipients);

	void decrypt(String sourceFile, String targetFile, Key<TKeyData> decryptionKey, String passphrase)
			throws InvalidPasswordException;

	/**
	 * Discover all key ids which can be used for decryption
	 */
	Set<String> findKeyIdsForDecryption(String filePathName);
}
