package org.pgpvault.gui.configpairs.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.pgpvault.gui.config.api.ConfigsBasePathResolver;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
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
	public <T> T find(String key, T defaultValue) {
		@SuppressWarnings("unchecked")
		T ret = (T) map.get(key);
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
