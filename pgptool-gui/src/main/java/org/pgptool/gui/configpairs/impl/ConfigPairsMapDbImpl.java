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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBException.SerializationError;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

public class ConfigPairsMapDbImpl implements ConfigPairs, InitializingBean {
	private static Logger log = Logger.getLogger(ConfigPairsMapDbImpl.class);

	@Autowired
	private ConfigsBasePathResolver configsBasePathResolver;

	private String configsBasepath = File.separator + "configs";
	private DB db;
	private HTreeMap<String, Object> map;

	@SuppressWarnings("unchecked")
	@Override
	public void afterPropertiesSet() throws Exception {
		ensureAllDirsCreated();
		String mapDbFilename = getFilesBasePath() + File.separator + "config-pairs.mapdb";
		log.debug("Creating mapDB at " + mapDbFilename);
		db = DBMaker.fileDB(mapDbFilename).transactionEnable().make();
		map = db.hashMap("config-pairs", Serializer.STRING, Serializer.JAVA).createOrOpen();
		makeSureWeCanReadAllSettings();
	}

	private void makeSureWeCanReadAllSettings() {
		// read all values -- forces DTO version check
		try {
			for (Object o : map.values()) {
				// forces value read
			}
		} catch (SerializationError se) {
			log.warn("Failed to read config pairs. Looks like outdated DTO version. Have to clear the whole map.", se);
			map.clear();
			db.commit();
		}
	}

	private String getFilesBasePath() {
		return configsBasePathResolver.getConfigsBasePath() + configsBasepath;
	}

	private void ensureAllDirsCreated() {
		File configsFolder = new File(getFilesBasePath());
		if (!configsFolder.exists() && !configsFolder.mkdirs()) {
			throw new RuntimeException("Failed to ensure all dirs for config files: " + configsFolder);
		}
	}

	@Override
	public void put(String key, Object value) {
		try {
			if (value == null) {
				map.remove(key);
				return;
			}

			map.put(key, value);
		} finally {
			db.commit();
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T find(String key, T defaultValue) {
		T ret = null;
		try {
			ret = (T) map.get(key);
		} catch (SerializationError se) {
			log.warn("Failed to read from config: " + key
					+ ". Looks like outdated DTO version. Have to clear the whole map.", se);
			map.clear();
			db.commit();
		} catch (Throwable t) {
			log.warn("Failed to read from config: " + key, t);
		}
		if (ret == null) {
			return defaultValue;
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> List<T> findAllWithPrefixedKey(String keyPrefix) {
		List<T> ret = new ArrayList<>();
		for (String key : map.getKeys()) {
			if (key.startsWith(keyPrefix)) {
				ret.add((T) map.get(key));
			}
		}
		return ret;
	}

}
