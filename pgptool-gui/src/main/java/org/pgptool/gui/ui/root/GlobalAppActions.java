package org.pgptool.gui.ui.root;

import javax.swing.Action;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;

public interface GlobalAppActions {
	Action getActionImportKey();

	Action getActionCreateKey();

	void triggerPrivateKeyExport(Key<? extends KeyData> key);
}
