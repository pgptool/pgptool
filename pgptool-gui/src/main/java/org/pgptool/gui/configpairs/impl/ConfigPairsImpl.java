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
package org.pgptool.gui.configpairs.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

/**
 * This is VERY simple map-based impl of this storage. It uses config repo and
 * performs write operation each time after single key-value pair change
 * 
 * @author Sergey Karpushin
 *
 */
public class ConfigPairsImpl implements ConfigPairs, InitializingBean {
	private static Logger log = Logger.getLogger(ConfigPairsImpl.class);

	@Autowired
	private ConfigRepository configRepository;
	private ConfigPairs oldConfigPairs;

	private ConfigPairsEnvelop configPairsEnvelop;

	private String clarification;
	private ConfigPairMigrator configPairMigrator;

	// TODO: Remove this whole Predicate idea after migration is completed
	public ConfigPairsImpl(String clarification, ConfigPairMigrator configPairMigrator) {
		this.clarification = clarification;
		this.configPairMigrator = configPairMigrator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO: Remove after Migration is completed -- we don't need to eagerly load
		// things here
		ensureLoadded();
	}

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
		configRepository.persist(configPairsEnvelop, clarification);
	}

	private void ensureLoadded() {
		if (configPairsEnvelop != null) {
			return;
		}

		configPairsEnvelop = configRepository.read(ConfigPairsEnvelop.class, clarification);
		if (configPairsEnvelop != null) {
			return;
		}

		configPairsEnvelop = new ConfigPairsEnvelop();
		// TODO: CLean-up this Migration logic
		Set<Entry<String, Object>> old = oldConfigPairs.getAll();
		if (old == null) {
			return;
		}

		for (Entry<String, Object> e : old) {
			if (!configPairMigrator.test(e)) {
				continue;
			}
			log.info("Migrated into config pairs " + clarification + " key " + e.getKey());
			Entry<String, Object> migrated = configPairMigrator.apply(e);
			configPairsEnvelop.put(migrated.getKey(), migrated.getValue());
		}
		log.info("Migrated config pairs " + clarification + " count: " + configPairsEnvelop.size());
		save();
	}

	@SuppressWarnings("unchecked")
	@Override
	public synchronized <T> T find(String key, T defaultValue) {
		ensureLoadded();
		T ret = (T) configPairsEnvelop.get(key);
		return ret != null ? ret : defaultValue;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> findAllWithPrefixedKey(String keyPrefix) {
		ensureLoadded();
		List<T> ret = new ArrayList<>();
		for (Entry<String, Object> entry : configPairsEnvelop.entrySet()) {
			if (entry.getKey().startsWith(keyPrefix)) {
				ret.add((T) entry.getValue());
			}
		}
		return ret;
	}

	public ConfigPairs getOldConfigPairs() {
		return oldConfigPairs;
	}

	@Required
	public void setOldConfigPairs(ConfigPairs oldConfigPairs) {
		this.oldConfigPairs = oldConfigPairs;
	}

	@Override
	public Set<Entry<String, Object>> getAll() {
		return configPairsEnvelop.entrySet();
	}
}
