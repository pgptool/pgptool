package org.pgptool.gui.config.api;

public interface ConfigRepository {
	<T> T read(Class<T> clazz);

	<T> T readOrConstruct(Class<T> clazz);

	<T> void persist(T object);

}
