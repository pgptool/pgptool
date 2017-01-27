package org.pgpvault.gui.configpairs.impl;

import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This is VERY simple map-based impl of this storage. It uses config repo and
 * performs write operation each time after single key-value pair change
 * 
 * @author sergeyk
 *
 */
public class ConfigPairsImpl implements ConfigPairs {
	@Autowired
	private ConfigRepository configRepository;

	private ConfigPairsEnvelop configPairsEnvelop;

	@Override
	public synchronized void put(String key, Object value) {
		ensureLoadded();
		if (value == null) {
			configPairsEnvelop.remove(key);
		} else {
			configPairsEnvelop.put(key, value);
		}
		save();
	}

	private void save() {
		configRepository.persist(configPairsEnvelop);
	}

	private void ensureLoadded() {
		if (configPairsEnvelop == null) {
			configPairsEnvelop = configRepository.readOrConstruct(ConfigPairsEnvelop.class);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T find(String key, T defaultValue) {
		ensureLoadded();
		T ret = (T) configPairsEnvelop.get(key);
		return ret != null ? ret : defaultValue;
	}

}
