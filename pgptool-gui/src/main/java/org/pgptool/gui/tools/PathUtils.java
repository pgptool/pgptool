package org.pgptool.gui.tools;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class PathUtils {
	public static Logger log = Logger.getLogger(PathUtils.class);

	public static String extractBasePath(String fileSystemItemPathName) {
		return FilenameUtils.getFullPathNoEndSeparator(fileSystemItemPathName);
	}

}
