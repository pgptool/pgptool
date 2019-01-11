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
