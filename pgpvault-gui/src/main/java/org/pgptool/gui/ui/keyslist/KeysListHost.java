package org.pgptool.gui.ui.keyslist;

import javax.swing.Action;

public interface KeysListHost {
	void handleClose();

	Action getActionImportKey();

	Action getActionCreateKey();
}
