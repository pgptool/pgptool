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
