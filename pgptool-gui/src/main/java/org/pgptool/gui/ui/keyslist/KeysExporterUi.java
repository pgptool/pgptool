package org.pgptool.gui.ui.keyslist;

import java.awt.Window;
import java.util.ArrayList;

import org.pgptool.gui.encryption.api.dto.Key;

public interface KeysExporterUi {

	void exportPrivateKey(Key key, Window parentWindow);

	void exportPublicKey(Key key, Window parentWindow);

	void exportPublicKeys(ArrayList<Key> keys, Window parentWindow);

}
