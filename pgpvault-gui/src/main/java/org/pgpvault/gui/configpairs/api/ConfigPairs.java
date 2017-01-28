package org.pgpvault.gui.configpairs.api;

import java.util.List;

/**
 * Simple service for key-value properties. name might a little bit unusual, but
 * it was by intention to avoid confusing it with encryption keys and key pairs.
 * 
 * @author sergeyk
 *
 */
public interface ConfigPairs {
	void put(String key, Object value);

	<T> T find(String key, T defaultValue);

	/**
	 * Will return values of all pairs where key is prefixed with keyPrefix
	 */
	<T> List<T> findAllWithPrefixedKey(String keyPrefix);
}
