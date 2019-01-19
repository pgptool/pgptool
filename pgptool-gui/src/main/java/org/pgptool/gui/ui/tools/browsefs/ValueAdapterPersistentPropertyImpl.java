/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
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
