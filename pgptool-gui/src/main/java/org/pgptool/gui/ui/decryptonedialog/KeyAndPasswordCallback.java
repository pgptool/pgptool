package org.pgptool.gui.ui.decryptonedialog;

import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;

public interface KeyAndPasswordCallback {
	public void onKeyPasswordResult(PasswordDeterminedForKey keyAndPassword);
}
