package org.pgptool.gui.configpairs.impl;

import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.pgptool.gui.decryptedlist.impl.MonitoringDecryptedFilesServiceImpl;

public class MonitoredDecryptedMigrationPredicate implements ConfigPairMigrator {
	@Override
	public boolean test(Entry<String, Object> e) {
		if (e.getKey().startsWith(MonitoringDecryptedFilesServiceImpl.PREFIX)) {
			return true;
		}

		return false;
	}

	@Override
	public Entry<String, Object> apply(Entry<String, Object> t) {
		return Pair.of(t.getKey().substring(MonitoringDecryptedFilesServiceImpl.PREFIX.length()), t.getValue());
	}
}
