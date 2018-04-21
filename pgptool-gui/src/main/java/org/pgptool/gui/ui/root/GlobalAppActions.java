package org.pgptool.gui.ui.root;

import javax.swing.Action;

import org.pgptool.gui.encryption.api.dto.Key;

public interface GlobalAppActions {
	Action getActionImportKey();

	Action getActionCreateKey();

	void triggerPrivateKeyExport(Key key);
}
