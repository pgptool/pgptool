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
package org.pgptool.gui.ui.mainframe;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;

import ru.skarpushin.swingpm.modelprops.table.LightweightTableModel;

public class DecryptedFilesModel implements LightweightTableModel<DecryptedFile> {
	public static final int COLUMN_ENCRYPTED_FILE = 0;
	public static final int COLUMN_DECRYPTED_FILE = 1;

	/**
	 * Absoilute pathname to File object
	 */
	private Map<String, File> cache = new HashMap<>();

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
		case COLUMN_ENCRYPTED_FILE:
			return Messages.get("term.encryptedFile");
		case COLUMN_DECRYPTED_FILE:
			return Messages.get("term.decryptedFile");
		default:
			throw new IllegalArgumentException("Wrong column index: " + columnIndex);
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public Object getValueAt(DecryptedFile r, int columnIndex) {
		if (r == null) {
			return "";
		}

		switch (columnIndex) {
		case COLUMN_ENCRYPTED_FILE:
			return buildStringForEncryptedFile(r);
		case COLUMN_DECRYPTED_FILE:
			return r.getDecryptedFile();
		default:
			throw new IllegalArgumentException("Wrong column index: " + columnIndex);
		}
	}

	private String buildStringForEncryptedFile(DecryptedFile r) {
		boolean isExists = getOrBuildFileFor(r.getEncryptedFile()).exists();
		if (isExists) {
			return r.getEncryptedFile();
		}
		return Messages.text("term.encryptedFile.notFound") + " " + r.getEncryptedFile();
	}

	private File getOrBuildFileFor(String filePathName) {
		File ret = cache.get(filePathName);
		if (ret == null) {
			cache.put(filePathName, ret = new File(filePathName));
		}
		return ret;
	}
}
