package org.pgptool.gui.ui.keyslist;

import java.awt.Window;
import java.util.ArrayList;

import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;

public interface KeysExporterUi {

	void exportPrivateKey(Key<KeyData> key, Window parentWindow);

	void exportPublicKey(Key<KeyData> key, Window parentWindow);

	void exportPublicKeys(ArrayList<Key<KeyData>> keys, Window parentWindow);

}
