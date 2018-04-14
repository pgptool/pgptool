package org.pgptool.gui.encryption.api;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class OutputStreamFactoryImpl implements OutputStreamFactory {

	@Override
	public OutputStream create(String fileName) throws FileNotFoundException {
		return new FileOutputStream(fileName, false);
	}

}
