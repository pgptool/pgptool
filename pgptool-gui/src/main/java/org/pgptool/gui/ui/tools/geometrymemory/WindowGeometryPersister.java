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
package org.pgptool.gui.ui.tools.geometrymemory;

import ru.skarpushin.swingpm.base.Detachable;

public interface WindowGeometryPersister extends Detachable {

	/**
	 * @return true if config values found and applied
	 */
	boolean restoreSize();

	// /**
	// * TBD: I think settings location forcibly could be dangerous, it might not
	// be
	// * what user expects. Let's closely look at it and I might revert this feature
	// *
	// * @return true if config values found and applied
	// */
	// boolean restoreLocation();
}
