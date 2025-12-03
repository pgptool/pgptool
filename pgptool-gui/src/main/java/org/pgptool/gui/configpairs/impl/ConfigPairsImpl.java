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
package org.pgptool.gui.configpairs.impl;

import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summerb.utils.DtoBase;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent;

/**
 * This is VERY simple map-based impl of this storage. It uses config repo and performs write
 * operation each time after single key-value pair change
 *
 * @author Sergey Karpushin
 */
public class ConfigPairsImpl implements ConfigPairs {
  private static final Logger log = LoggerFactory.getLogger(ConfigPairsImpl.class);

  private final ConfigRepository configRepository;
  private final EventBus eventBus;

  private ConfigPairsEnvelop configPairsEnvelop;
  private final String clarification;
  private Gson gson = new Gson();

  public ConfigPairsImpl(
      ConfigRepository configRepository, EventBus eventBus, String clarification) {
    this.configRepository = configRepository;
    this.eventBus = eventBus;
    this.clarification = clarification;
  }

  @Override
  public synchronized void put(String key, Object value) {
    if (log.isDebugEnabled()) {
      String valueStr = value == null ? "(null)" : gson.toJson(value);
      log.debug("Saving \"{}\" property \"{}\" value: {}", clarification, key, valueStr);
    }

    if (value == null) {
      Object removed = getConfigPairsEnvelop().remove(key);
      if (removed != null & removed instanceof DtoBase) {
        eventBus.post(EntityChangedEvent.removedObject((DtoBase) removed));
      }
    } else {
      Object previous = getConfigPairsEnvelop().put(key, value);

      if (previous != null & value instanceof DtoBase) {
        eventBus.post(EntityChangedEvent.updated((DtoBase) value));
      } else if (value instanceof DtoBase) {
        eventBus.post(EntityChangedEvent.added((DtoBase) value));
      }
    }
    save();
  }

  private void save() {
    configRepository.persist(getConfigPairsEnvelop(), clarification);
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized <T> T find(String key, T defaultValue) {
    T ret = (T) getConfigPairsEnvelop().get(key);
    return ret != null ? ret : defaultValue;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> List<T> findAllWithPrefixedKey(String keyPrefix) {
    List<T> ret = new ArrayList<>();
    for (Entry<String, Object> entry : getConfigPairsEnvelop().entrySet()) {
      if (entry.getKey().startsWith(keyPrefix)) {
        ret.add((T) entry.getValue());
      }
    }
    return ret;
  }

  @Override
  public synchronized Set<Entry<String, Object>> getAll() {
    return new HashSet<>(getConfigPairsEnvelop().entrySet());
  }

  public ConfigPairsEnvelop getConfigPairsEnvelop() {
    if (configPairsEnvelop == null) {
      configPairsEnvelop =
          configRepository.readOrConstruct(ConfigPairsEnvelop.class, clarification);
    }
    return configPairsEnvelop;
  }

  public void setGson(Gson gson) {
    this.gson = gson;
  }
}
