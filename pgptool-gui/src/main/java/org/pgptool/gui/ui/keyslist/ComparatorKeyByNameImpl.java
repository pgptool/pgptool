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
