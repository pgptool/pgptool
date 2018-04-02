package org.pgptool.gui.configpairs.impl;

import java.util.Map.Entry;

import org.pgptool.gui.decryptedlist.impl.MonitoringDecryptedFilesServiceImpl;
import org.pgptool.gui.encryption.implpgp.PgpKeysRing;
import org.pgptool.gui.encryptionparams.impl.EncryptionParamsStorageImpl;
import org.pgptool.gui.ui.decryptone.DecryptOnePm;

public class AppPropertiesMigrationPredicate implements ConfigPairMigrator {
	@Override
	public boolean test(Entry<String, Object> e) {
		// migrate all except:
		if (e.getKey().startsWith(MonitoringDecryptedFilesServiceImpl.PREFIX)) {
			return false;
		}
		if (e.getKey().equals(PgpKeysRing.class.getName())) {
			return false;
		}
		if (e.getKey().startsWith(DecryptOnePm.CONFIG_PAIR_BASE)) {
			return false;
		}
		if (e.getKey().startsWith(EncryptionParamsStorageImpl.CONFIG_PAIR_BASE)) {
			return false;
		}

		return true;
	}

	@Override
	public Entry<String, Object> apply(Entry<String, Object> t) {
		return t;
	}
}
