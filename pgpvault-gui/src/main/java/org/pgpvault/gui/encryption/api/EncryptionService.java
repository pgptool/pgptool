package org.pgpvault.gui.encryption.api;

import java.util.Collection;

import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;

public interface EncryptionService<TKeyData extends KeyData> {
	void encrypt(String sourceFile, String targetFile, Collection<Key<TKeyData>> recipients);
}
