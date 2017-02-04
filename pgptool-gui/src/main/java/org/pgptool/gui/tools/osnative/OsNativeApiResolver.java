package org.pgptool.gui.tools.osnative;

public class OsNativeApiResolver {
	private static OsNativeApi INSTANCE;
	private static Object SYNC = new Object();

	public static OsNativeApi resolve() {
		if (INSTANCE != null) {
			return INSTANCE;
		}

		synchronized (SYNC) {
			if (INSTANCE != null) {
				return INSTANCE;
			}

			INSTANCE = buildInstanceForCurrentOs();
			return INSTANCE;
		}
	}

	private static OsNativeApi buildInstanceForCurrentOs() {
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			return new OsNativeApiWindowsImpl();
		}
		return new OsNativeApiDefaultImpl();
	}
}
