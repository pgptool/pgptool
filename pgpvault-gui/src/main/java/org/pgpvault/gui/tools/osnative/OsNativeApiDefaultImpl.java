package org.pgpvault.gui.tools.osnative;

public class OsNativeApiDefaultImpl implements OsNativeApi {
	@Override
	public String[] getCommandLineArguments(String[] fallBackTo) {
		return fallBackTo;
	}
}