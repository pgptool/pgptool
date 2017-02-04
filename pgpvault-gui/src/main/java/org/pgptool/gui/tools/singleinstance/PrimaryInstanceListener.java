package org.pgptool.gui.tools.singleinstance;

public interface PrimaryInstanceListener {
	void handleArgsFromOtherInstance(String[] args);
}
