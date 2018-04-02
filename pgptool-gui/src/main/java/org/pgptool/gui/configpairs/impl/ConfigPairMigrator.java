package org.pgptool.gui.configpairs.impl;

import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

public interface ConfigPairMigrator
		extends Predicate<Entry<String, Object>>, Function<Map.Entry<String, Object>, Map.Entry<String, Object>> {

}
