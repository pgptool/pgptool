package org.pgptool.gui.ui.tools.browsefs;

import org.pgptool.gui.configpairs.api.ConfigPairs;

import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

public class ValueAdapterPersistentPropertyImpl<T> implements ValueAdapter<T> {
	private ConfigPairs configPairs;
	private String propertyName;
	private T defaultValue;

	public ValueAdapterPersistentPropertyImpl(ConfigPairs configPairs, String propertyName, T defaultValue) {
		this.configPairs = configPairs;
		this.propertyName = propertyName;
		this.defaultValue = defaultValue;
	}

	@Override
	public T getValue() {
		T ret = configPairs.find(propertyName, defaultValue);
		return ret;
	}

	@Override
	public void setValue(T value) {
		configPairs.put(propertyName, value);
	}

}
