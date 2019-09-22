package org.pgptool.gui.usage.dto;

import java.io.Serializable;

public class ConfigPairUsage implements Serializable {
	private static final long serialVersionUID = -200170723205714555L;
	private String storage;
	private String key;
	private Object value;

	public ConfigPairUsage() {
	}

	public ConfigPairUsage(String storage, String key, Object value) {
		this.storage = storage;
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public String getStorage() {
		return storage;
	}

	public void setStorage(String storage) {
		this.storage = storage;
	}

}
