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
package org.pgptool.gui.encryption.api;

import java.util.List;
import java.util.Set;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.MatchedKey;

public interface KeyRingService {
	List<Key> readKeys();

	void addKey(Key key);

	void removeKey(Key key);

	/**
	 * This method will find all keys that are compatible with IDs provided and
	 * suitable for decryption. It might not be the keyId itself, but ID that is
	 * supported by the key. Which means it doens't necessarily true that (pseudo
	 * code) keysIds.containsAll(return.getIds)
	 * 
	 * @param keysIds
	 *            list of ids needs to be found.
	 * @return list of keys or empty array if none found
	 */
	List<MatchedKey> findMatchingDecryptionKeys(Set<String> keysIds);

	List<Key> findMatchingKeys(Set<String> keysIds);

	Key findKeyById(String keyId);

}
