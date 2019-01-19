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
package org.pgptool.gui.tempfolderfordecrypted.api;

import org.summerb.approaches.validation.FieldValidationException;

/**
 * This interface provides methods to work with temporary folder for temporarily
 * decrypted files
 * 
 * @author Sergey Karpushin
 *
 */
public interface DecryptedTempFolder {
	String getTempFolderBasePath();

	void setTempFolderBasePath(String newValue) throws FieldValidationException;
}
