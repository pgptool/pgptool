package org.pgptool.gui.autoupdate.api;

import org.pgptool.gui.app.GenericException;

public interface NewVersionChecker {
	/**
	 * @return information about update or null if current version is same or newer
	 * @throws GenericException
	 *             if operation failed
	 */
	UpdatePackageInfo findNewUpdateIfAvailable() throws GenericException;

	String getCurrentVersion();
}
