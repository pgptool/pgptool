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
package org.pgptool.gui.configpairs.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple service for key-value properties. name might a little bit unusual, but
 * it was by intention to avoid confusing it with encryption keys and key pairs.
 * 
 * @author Sergey Karpushin
 *
 */
public interface ConfigPairs {
	void put(String key, Object value);

	<T> T find(String key, T defaultValue);

	/**
	 * Will return values of all pairs where key is prefixed with keyPrefix
	 */
	<T> List<T> findAllWithPrefixedKey(String keyPrefix);

	Set<Map.Entry<String, Object>> getAll();
}
