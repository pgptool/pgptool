package org.pgptool.gui.ui.keyslist;

import java.util.Comparator;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;

@SuppressWarnings("rawtypes")
public class ComparatorKeyByNameImpl<T extends KeyData> implements Comparator<Key<T>> {
	@Override
	public int compare(Key o1, Key o2) {
		if (isNull(o1) && isNull(o2)) {
			return 0;
		}
		if (isNull(o1)) {
			return -1;
		}
		if (isNull(o2)) {
			return 1;
		}
		return o1.getKeyInfo().getUser().compareToIgnoreCase(o2.getKeyInfo().getUser());
	}

	private boolean isNull(Key o1) {
		return (o1 == null || o1.getKeyInfo() == null || o1.getKeyInfo().getUser() == null);
	}
}
