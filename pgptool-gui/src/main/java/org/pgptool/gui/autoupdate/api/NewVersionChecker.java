/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.pgptool.gui.autoupdate.api;

import org.pgptool.gui.app.GenericException;

public interface NewVersionChecker {
	public static String VERSION_UNRESOLVED = "unresolved";

	/**
	 * @return information about update or null if current version is same or newer
	 * @throws GenericException if operation failed
	 */
	UpdatePackageInfo findNewUpdateIfAvailable() throws GenericException;

	String getCurrentVersion();
}
