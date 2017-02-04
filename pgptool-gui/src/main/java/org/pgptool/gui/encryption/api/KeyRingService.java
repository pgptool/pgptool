package org.pgptool.gui.encryption.api;

import java.util.List;
import java.util.Set;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;

public interface KeyRingService<TKeyData extends KeyData> {
	List<Key<TKeyData>> readKeys();

	void addKey(Key<TKeyData> key);

	void removeKey(Key<TKeyData> key);

	/**
	 * This method will find all keys that are compatible with IDs provided and
	 * suitable for decryption. It might not be the keyId itself, but ID that is
	 * supported by the key. Which means it doens't necessarily true that
	 * (pseudo code) keysIds.containsAll(return.getIds)
	 * 
	 * @param keysIds
	 *            list of ids needs to be found.
	 * @return list of keys or empty array if none found
	 */
	List<Key<TKeyData>> findMatchingKeysByAlternativeIds(Set<String> keysIds);
}
