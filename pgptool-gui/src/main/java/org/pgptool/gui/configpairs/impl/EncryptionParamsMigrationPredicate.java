package org.pgptool.gui.configpairs.impl;

import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.pgptool.gui.encryptionparams.impl.EncryptionParamsStorageImpl;

public class EncryptionParamsMigrationPredicate implements ConfigPairMigrator {
	@Override
	public boolean test(Entry<String, Object> e) {
		if (e.getKey().startsWith(EncryptionParamsStorageImpl.CONFIG_PAIR_BASE)) {
			return true;
		}

		return false;
	}

	@Override
	public Entry<String, Object> apply(Entry<String, Object> t) {
		return Pair.of(t.getKey().substring(EncryptionParamsStorageImpl.CONFIG_PAIR_BASE.length()), t.getValue());
	}
}
