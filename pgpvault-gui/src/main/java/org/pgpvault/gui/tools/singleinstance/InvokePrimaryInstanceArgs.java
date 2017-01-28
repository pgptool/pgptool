package org.pgpvault.gui.tools.singleinstance;

import java.io.Serializable;

public class InvokePrimaryInstanceArgs implements Serializable {
	private static final long serialVersionUID = -1935402056245763044L;

	private String[] commandLineArgs;

	public InvokePrimaryInstanceArgs(String[] commandLineArgs) {
		this.commandLineArgs = commandLineArgs;
	}

	public InvokePrimaryInstanceArgs() {
	}

	public String[] getCommandLineArgs() {
		return commandLineArgs;
	}

	public void setCommandLineArgs(String[] commandLineArgs) {
		this.commandLineArgs = commandLineArgs;
	}
}
