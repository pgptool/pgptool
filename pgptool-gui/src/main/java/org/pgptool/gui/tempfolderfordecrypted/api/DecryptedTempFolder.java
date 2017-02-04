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
