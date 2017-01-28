package org.pgpvault.gui.tools.singleinstance;

public interface PrimaryInstanceListener {
	void handleArgsFromOtherInstance(String[] args);
}
