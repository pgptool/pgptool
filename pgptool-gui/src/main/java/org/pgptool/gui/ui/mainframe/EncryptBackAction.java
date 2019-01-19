/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
package org.pgptool.gui.ui.mainframe;

import org.pgptool.gui.decryptedlist.api.DecryptedFile;

public class EncryptBackAction {
	private DecryptedFile decryptedFile;

	public EncryptBackAction(DecryptedFile decryptedFile) {
		this.decryptedFile = decryptedFile;
	}

	public DecryptedFile getDecryptedFile() {
		return decryptedFile;
	}

	public void setDecryptedFile(DecryptedFile decryptedFile) {
		this.decryptedFile = decryptedFile;
	}
}
