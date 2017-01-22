package org.pgpvault.gui.tools;

import java.io.File;

import org.apache.log4j.Logger;

public class PathUtils {
	public static Logger log = Logger.getLogger(PathUtils.class);

	public static String extractBasePath(String fileSystemItemPathName) {
		try {
			String dir = "";
			File file = new File(fileSystemItemPathName);
			if (file.exists()) {
				if (file.isDirectory()) {
					dir = file.getAbsolutePath();
				} else {
					dir = file.getParentFile().getAbsolutePath();
				}
			} else if (file.getParentFile().exists()) {
				dir = file.getParentFile().getAbsolutePath();
			}
			return dir;
		} catch (Throwable t) {
			log.warn("Failed to extract base path from: " + fileSystemItemPathName, t);
			return "";
		}
	}

}
