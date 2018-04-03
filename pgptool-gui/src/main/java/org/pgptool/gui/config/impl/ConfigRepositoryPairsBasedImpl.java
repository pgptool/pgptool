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
package org.pgptool.gui.config.impl;

import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.common.DtoBase;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

/**
 * This impl doesn't have it's own storage it just uses {@link ConfigPairs}
 * 
 * @author Sergey Karpushin
 *
 */
public class ConfigRepositoryPairsBasedImpl implements ConfigRepository {
	private ConfigPairs configPairs;
	@Autowired
	private EventBus eventBus;

	@Override
	public <T extends DtoBase> void persist(T object) {
		throw new IllegalStateException("Not supported. Migrating.");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends DtoBase> T read(Class<T> clazz) {
		try {
			Preconditions.checkArgument(clazz != null, "Class must be provided");
			return (T) configPairs.find(clazz.getName(), null);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to read object of class " + clazz, t);
		}
	}

	@Override
	public <T extends DtoBase> T readOrConstruct(Class<T> clazz) {
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

	public ConfigPairs getConfigPairs() {
		return configPairs;
	}

	@Autowired
	public void setConfigPairs(ConfigPairs configPairs) {
		this.configPairs = configPairs;
	}

	@Override
	public <T extends DtoBase> T read(Class<T> clazz, String clarification) {
		throw new IllegalStateException("Operation is not supported");
	}

	@Override
	public <T extends DtoBase> T readOrConstruct(Class<T> clazz, String clarification) {
		throw new IllegalStateException("Operation is not supported");
	}

	@Override
	public <T extends DtoBase> void persist(T object, String clarification) {
		throw new IllegalStateException("Operation is not supported");
	}
}
