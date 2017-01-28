package org.pgpvault.gui.ui.mainframe;

import javax.swing.Action;

public interface MainFrameHost {
	void handleExitApp();

	Action getActionShowAboutInfo();

	Action getActionImportKey();

	Action getActionShowKeysList();

	Action getActionForEncrypt();

	Action getActionForDecrypt();

	Action getActionChangeFolderForDecrypted();

	void openEncryptDialogFor(String decryptedFile);
}
