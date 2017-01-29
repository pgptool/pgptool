package org.pgpvault.gui.config.impl;

import java.io.File;

import org.apache.log4j.Logger;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.config.api.ConfigsBasePathResolver;
import org.pgpvault.gui.configpairs.impl.ConfigPairsMapDbImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

/**
 * 
 * @author sergeyk
 *
 * @deprecated it's silly to have own impl on top of MapDB if we can reuse
 *             {@link ConfigPairsMapDbImpl}
 */
@Deprecated
public class ConfigRepositoryMapDbImpl implements ConfigRepository, InitializingBean {
	private static Logger log = Logger.getLogger(ConfigRepositoryMapDbImpl.class);
	private ConfigsBasePathResolver configsBasePathResolver;
	private String configsBasepath = File.separator + "configs";
	private EventBus eventBus;
	private DB db;
	private HTreeMap<String, Object> map;

	@SuppressWarnings("unchecked")
	@Override
	public void afterPropertiesSet() throws Exception {
		ensureAllDirsCreated();
		String mapDbFilename = getFilesBasePath() + File.separator + "config-repo.mapdb";
		log.debug("Creating mapDB at " + mapDbFilename);
		db = DBMaker.fileDB(mapDbFilename).transactionEnable().make();
		map = db.hashMap("config-repo", Serializer.STRING, Serializer.JAVA).createOrOpen();
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
	public <T> void persist(T object) {
		try {
			Preconditions.checkArgument(object != null, "Can't persist null object");
			map.put(object.getClass().getName(), object);
			db.commit();
			eventBus.post(EntityChangedEvent.updated(object));
		} catch (Throwable t) {
			throw new RuntimeException("Failed to persist object " + object, t);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T read(Class<T> clazz) {
		try {
			Preconditions.checkArgument(clazz != null, "Class must be provided");
			return (T) map.get(clazz.getName());
		} catch (Throwable t) {
			throw new RuntimeException("Failed to read object of class " + clazz, t);
		}
	}

	@Override
	public <T> T readOrConstruct(Class<T> clazz) {
		T result = read(clazz);
		if (result == null) {
			try {
				result = clazz.newInstance();
			} catch (Throwable t) {
				throw new RuntimeException("Failed to create new instance of " + clazz, t);
			}
		}

		return result;
	}

	public ConfigsBasePathResolver getConfigsBasePathResolver() {
		return configsBasePathResolver;
	}

	@Autowired
	public void setConfigsBasePathResolver(ConfigsBasePathResolver configsBasePathResolver) {
		this.configsBasePathResolver = configsBasePathResolver;
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	@Autowired
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

}
