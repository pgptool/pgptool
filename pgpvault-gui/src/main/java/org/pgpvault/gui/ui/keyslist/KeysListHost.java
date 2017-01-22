package org.pgpvault.gui.ui.keyslist;

import javax.swing.Action;

public interface KeysListHost {
	void handleClose();

	Action getActionImportKey();
}
