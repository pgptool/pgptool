package org.pgpvault.gui.encryption.api;

import java.util.List;

import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;

public interface KeyRingService<TKeyData extends KeyData> {
	List<Key<TKeyData>> readKeys();

	void addKey(Key<TKeyData> key);

	void removeKey(Key<TKeyData> key);
}
