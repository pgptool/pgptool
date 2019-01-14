package org.pgptool.gui.encryption.implpgp;

import org.summerb.utils.exceptions.GenericException;

public class SymmetricEncryptionIsNotSupportedException extends GenericException {
	private static final long serialVersionUID = 4518578937183956465L;

	public SymmetricEncryptionIsNotSupportedException() {
		super("exception.symmetricEncryptionNotSupported");
	}
}
