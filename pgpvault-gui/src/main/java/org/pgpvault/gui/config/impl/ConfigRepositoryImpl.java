package org.pgpvault.gui.config.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.log4j.Logger;
import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.config.api.ConfigsBasePathResolver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class ConfigRepositoryImpl implements ConfigRepository, InitializingBean {
	private static Logger log = Logger.getLogger(ConfigRepositoryImpl.class);
	private ConfigsBasePathResolver configsBasePathResolver;
	private String configsBasepath = File.separator + "configs";
	private EventBus eventBus;

	@Override
	public void afterPropertiesSet() throws Exception {
		ensureAllDirsCreated();
	}

	private void ensureAllDirsCreated() {
		File configsFolder = new File(configsBasePathResolver.getConfigsBasePath() + configsBasepath);
		if (!configsFolder.exists() && !configsFolder.mkdirs()) {
			throw new RuntimeException("Failed to ensure all dirs for config files: " + configsFolder);
		}
	}

	@Override
	public <T> void persist(T object) {
		try {
			Preconditions.checkArgument(object != null, "Can't persist null object");
			String filename = buildFilenameForClass(object.getClass());
			writeObject(object, filename);
			eventBus.post(EntityChangedEvent.updated(object));
		} catch (Throwable t) {
			throw new RuntimeException("Failed to persist object " + object, t);
		}
	}

	private String buildFilenameForClass(Class<?> clazz) {
		return configsBasePathResolver.getConfigsBasePath() + configsBasepath + File.separator + clazz.getSimpleName();
	}

	@Override
	public <T> T read(Class<T> clazz) {
		try {
			Preconditions.checkArgument(clazz != null, "Class must be provided");
			String filename = buildFilenameForClass(clazz);
			if (!new File(filename).exists()) {
				return null;
			}
			return readObject(filename);
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

	@SuppressWarnings("unchecked")
	public static <T> T readObject(String sourceFile) {
		ObjectInputStream ois = null;
		try {
			File file = new File(sourceFile);
			if (!file.exists()) {
				return null;
			}

			FileInputStream fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			T ret = (T) ois.readObject();
			ois.close();
			return ret;
		} catch (Throwable t) {
			log.warn("Failed to read " + sourceFile, t);
			return null;
		} finally {
			safeClose(ois);
		}
	}

	public static void safeClose(ObjectInputStream fis) {
		if (fis != null) {
			try {
				fis.close();
			} catch (Throwable t) {
				// don't care
			}
		}
	}

	public static void writeObject(Object o, String destinationFile) {
		ObjectOutputStream oos = null;
		try {
			log.trace(String.format("Persisting %s to %s", o, destinationFile));

			File file = new File(destinationFile);
			if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
				throw new RuntimeException("Failed to create all parent directories");
			}

			FileOutputStream fout = new FileOutputStream(file);
			oos = new ObjectOutputStream(fout);
			oos.writeObject(o);
			oos.flush();
		} catch (Throwable t) {
			throw new RuntimeException("Failed to write config: " + destinationFile, t);
		} finally {
			safeClose(oos);
		}
	}

	public static void safeClose(ObjectOutputStream fis) {
		if (fis != null) {
			try {
				fis.close();
			} catch (Throwable t) {
				// don't care
			}
		}
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
