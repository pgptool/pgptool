package org.pgptool.gui.encryption.api;

import javax.xml.bind.ValidationException;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;

public interface KeyFilesOperations<TKeyData extends KeyData> {
	Key<TKeyData> readKeyFromFile(String filePathName) throws ValidationException;

	void exportPublicKey(Key<TKeyData> key, String targetFilePathname);

	void exportPrivateKey(Key<TKeyData> key, String targetFilePathname);
}
