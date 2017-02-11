/*******************************************************************************
 *     PGPTool is a desktop application for pgp encryption/decryption
 *     Copyright (C) 2017 Sergey Karpushin
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.pgptool.gui.ui.keyslist;

import javax.swing.Action;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;

public abstract class KeysTablePm extends PresentationModelBase {
	abstract public ModelTablePropertyAccessor<Key<KeyData>> getKeys();

	public ModelPropertyAccessor<Boolean> getHasData() {
		return null;
	}

	public ModelPropertyAccessor<Key<KeyData>> getSelectedRow() {
		return null;
	}

	public Action getActionDelete() {
		return null;
	}

	public Action getActionForRowDoubleClick() {
		return null;
	}

	public Action[] getContextMenuActions() {
		return null;
	}
}
