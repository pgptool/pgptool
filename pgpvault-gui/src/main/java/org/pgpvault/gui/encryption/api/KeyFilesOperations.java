package org.pgpvault.gui.encryption.api;

import javax.xml.bind.ValidationException;

import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;

public interface KeyFilesOperations<TKeyData extends KeyData> {
	Key<TKeyData> readKeyFromFile(String filePathName) throws ValidationException;
}
