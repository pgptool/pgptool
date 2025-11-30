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
package org.pgptool.gui.ui.tools.geometrymemory;

import com.google.common.base.Preconditions;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.springframework.util.StringUtils;

/**
 * This implementation will distinguish between different monitors configuration and based on that
 * will put properties in different "buckets"
 *
 * @author sergeyk
 */
public class ConfigPairsMonitorsDependentImpl implements ConfigPairs {

  private final ConfigPairs configPairs;

  public ConfigPairsMonitorsDependentImpl(ConfigPairs configPairs) {
    Preconditions.checkArgument(configPairs != null);

    this.configPairs = configPairs;
  }

  public static String getMonitorsConfiguation() {
    StringBuilder sb = new StringBuilder();
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (GraphicsDevice gd : ge.getScreenDevices()) {
      DisplayMode dm = gd.getDisplayMode();
      if (!sb.isEmpty()) {
        sb.append(",");
      }
      sb.append("\'");
      sb.append(gd.getIDstring());
      sb.append("\'");
      // NOTE: We can use getIDstring for overall "key" creation but we can't parse it
      // because it's specific to each JRE vendor, we can't rely on it's format
      sb.append("_");
      sb.append(dm.getWidth());
      sb.append("x");
      sb.append(dm.getHeight());
    }

    return sb.toString();
  }

  @Override
  public void put(String key, Object value) {
    Preconditions.checkArgument(StringUtils.hasText(key));
    // log.debug("Saving: " + buildKey(key) + "=" + value);
    configPairs.put(buildKey(key), value);
  }

  @Override
  public <T> T find(String key, T defaultValue) {
    Preconditions.checkArgument(StringUtils.hasText(key));
    return configPairs.find(buildKey(key), defaultValue);
  }

  @Override
  public <T> List<T> findAllWithPrefixedKey(String keyPrefix) {
    Preconditions.checkArgument(StringUtils.hasText(keyPrefix));
    return configPairs.findAllWithPrefixedKey(buildKey(keyPrefix));
  }

  private String buildKey(String key) {
    String prefix = getMonitorsConfiguation() + "$";
    if (key == null) {
      return prefix;
    }
    return prefix + key;
  }

  @Override
  public Set<Entry<String, Object>> getAll() {
    String prefix = buildKey(null);
    return configPairs.getAll().stream()
        .filter(x -> x.getKey().startsWith(prefix))
        .map(x -> Pair.of(x.getKey().substring(prefix.length()), x.getValue()))
        .collect(Collectors.toSet());
  }
}
