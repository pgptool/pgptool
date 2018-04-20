package org.pgptool.gui.ui.tools.geometrymemory;

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

import com.google.common.base.Preconditions;

/**
 * This implementation will distinguish between different monitors configuration
 * and based on that will put properties in different "buckets"
 * 
 * @author sergeyk
 *
 */
public class ConfigPairsMonitorsDependentImpl implements ConfigPairs {
	// private static Logger log =
	// Logger.getLogger(ConfigPairsMonitorsDependentImpl.class);

	private ConfigPairs configPairs;

	public ConfigPairsMonitorsDependentImpl(ConfigPairs configPairs) {
		Preconditions.checkArgument(configPairs != null);

		this.configPairs = configPairs;
	}

	public static String getMonitorsConfiguation() {
		StringBuilder sb = new StringBuilder();
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		for (GraphicsDevice gd : ge.getScreenDevices()) {
			DisplayMode dm = gd.getDisplayMode();
			if (sb.length() > 0) {
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

		String r = sb.toString();
		return r;
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
		return configPairs.getAll().stream().filter(x -> x.getKey().startsWith(prefix))
				.map(x -> Pair.of(x.getKey().substring(prefix.length()), x.getValue())).collect(Collectors.toSet());
	}
}
