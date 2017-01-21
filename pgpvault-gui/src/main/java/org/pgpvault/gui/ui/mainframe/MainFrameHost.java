package org.pgpvault.gui.ui.mainframe;

import javax.swing.Action;

public interface MainFrameHost {
	void handleExitApp();

	Action getActionShowAboutInfo();
}
