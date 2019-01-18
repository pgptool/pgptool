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

import java.io.File;
import java.util.List;

import org.pgptool.gui.encryption.api.dto.Key;
import org.summerb.approaches.validation.FieldValidationException;

public interface KeyFilesOperations {
	final String FN_PASSWORD = "password";

	List<Key> readKeysFromFile(File file);

	List<Key> readKeysFromText(String text);

	void exportPublicKey(Key key, String targetFilePathname);

	void exportPrivateKey(Key key, String targetFilePathname);

	void validateDecryptionKeyPassword(String requestedKeyId, Key key, String password) throws FieldValidationException;

	Key readKeyFromFile(String fileName);
}
