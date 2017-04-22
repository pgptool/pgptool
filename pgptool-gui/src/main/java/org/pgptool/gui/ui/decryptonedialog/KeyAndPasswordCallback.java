package org.pgptool.gui.ui.decryptonedialog;

import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;

public interface KeyAndPasswordCallback<TKeyData extends KeyData> {
	public void onKeyPasswordResult(PasswordDeterminedForKey<TKeyData> keyAndPassword);
}
