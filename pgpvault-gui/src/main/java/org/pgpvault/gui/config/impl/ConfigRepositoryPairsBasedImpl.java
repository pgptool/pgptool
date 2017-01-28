package org.pgpvault.gui.config.impl;

import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.configpairs.api.ConfigPairs;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

/**
 * This impl doesn't have it's own storage it just uses {@link ConfigPairs}
 * 
 * @author sergeyk
 *
 */
public class ConfigRepositoryPairsBasedImpl implements ConfigRepository {
	@Autowired
	private ConfigPairs configPairs;
	@Autowired
	private EventBus eventBus;

	@Override
	public <T> void persist(T object) {
		try {
			Preconditions.checkArgument(object != null, "Can't persist null object");
			configPairs.put(object.getClass().getName(), object);
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
			return (T) configPairs.find(clazz.getName(), null);
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
}
