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

import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import org.pgptool.gui.bkgoperation.ProgressHandler;
import org.pgptool.gui.bkgoperation.UserRequestedCancellationException;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.summerb.approaches.security.api.exceptions.InvalidPasswordException;

public interface EncryptionService {
	void encrypt(String sourceFile, String targetFile, Collection<Key> recipients,
			ProgressHandler optionalProgressHandler, InputStreamSupervisor optionalInputStreamSupervisor,
			OutputStreamSupervisor optionalOutputStreamSupervisor) throws UserRequestedCancellationException;

	String encryptText(String sourceText, Collection<Key> recipients);

	void decrypt(String sourceFile, String targetFile, PasswordDeterminedForKey keyAndPassword,
			ProgressHandler optionalProgressHandler, OutputStreamSupervisor optionalOutputStreamSupervisor)
			throws InvalidPasswordException, UserRequestedCancellationException;

	String decryptText(String encryptedText, PasswordDeterminedForKey keyAndPassword) throws InvalidPasswordException;

	/**
	 * Discover all key ids which can be used for decryption
	 */
	Set<String> findKeyIdsForDecryption(String filePathName);

	/**
	 * @param inputStream
	 *            input stream for encrypted text
	 */
	Set<String> findKeyIdsForDecryption(InputStream inputStream);

	/**
	 * This method "pre-decrypts" file only to get initial file name that was
	 * encrypted
	 * 
	 * @param encryptedFile
	 *            encrypted file
	 * @param keyAndPassword
	 *            key and password to use for decryption
	 * @return initial file name that was encrypted (name only, no path)
	 */
	String getNameOfFileEncrypted(String encryptedFile, PasswordDeterminedForKey keyAndPassword)
			throws InvalidPasswordException;
}
