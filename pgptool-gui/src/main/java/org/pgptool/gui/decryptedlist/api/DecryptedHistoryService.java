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
package org.pgptool.gui.decryptedlist.api;

import java.util.List;

public interface DecryptedHistoryService {
	/**
	 * Register decrypted file for history
	 */
	void add(DecryptedFile decryptedFile);

	/**
	 * Forget about decrypted file. does NOT remove file from disk
	 * 
	 * @param depcryptedFilePathname
	 *            TODO
	 */
	void remove(String depcryptedFilePathname);

	List<DecryptedFile> getDecryptedFiles();
}
