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
package org.pgptool.gui.decryptedlist.api;

import org.pgptool.gui.filecomparison.Fingerprint;
import org.summerb.approaches.jdbccrud.common.DtoBase;

public class DecryptedFile implements DtoBase {
	private static final long serialVersionUID = -6339203835731870415L;

	private String encryptedFile;
	private String decryptedFile;

	private Fingerprint encryptedFileFingerprint;
	private Fingerprint decryptedFileFingerprint;

	public DecryptedFile() {
	}

	public DecryptedFile(String encryptedFile, String decryptedFile) {
		this.encryptedFile = encryptedFile;
		this.decryptedFile = decryptedFile;
	}

	public String getEncryptedFile() {
		return encryptedFile;
	}

	public void setEncryptedFile(String encryptedFile) {
		this.encryptedFile = encryptedFile;
	}

	public String getDecryptedFile() {
		return decryptedFile;
	}

	public void setDecryptedFile(String decryptedFile) {
		this.decryptedFile = decryptedFile;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((decryptedFile == null) ? 0 : decryptedFile.hashCode());
		result = prime * result + ((encryptedFile == null) ? 0 : encryptedFile.hashCode());
		return result;
	}

	// NOTE: This functionality is not using baseline CRC info because it's used to
	// locate file in a monitored decrypted. See MainFramePm::onRowChangedEvent
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DecryptedFile other = (DecryptedFile) obj;
		if (decryptedFile == null) {
			if (other.decryptedFile != null)
				return false;
		} else if (!decryptedFile.equals(other.decryptedFile))
			return false;
		if (encryptedFile == null) {
			if (other.encryptedFile != null)
				return false;
		} else if (!encryptedFile.equals(other.encryptedFile))
			return false;
		return true;
	}

	public Fingerprint getEncryptedFileFingerprint() {
		return encryptedFileFingerprint;
	}

	public void setEncryptedFileFingerprint(Fingerprint encryptedFileState) {
		this.encryptedFileFingerprint = encryptedFileState;
	}

	public Fingerprint getDecryptedFileFingerprint() {
		return decryptedFileFingerprint;
	}

	public void setDecryptedFileFingerprint(Fingerprint decryptedFileState) {
		this.decryptedFileFingerprint = decryptedFileState;
	}
}
