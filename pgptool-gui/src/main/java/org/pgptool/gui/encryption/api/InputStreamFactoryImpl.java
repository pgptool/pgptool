package org.pgptool.gui.encryption.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class InputStreamFactoryImpl implements InputStreamFactory {

	@Override
	public InputStream create(String fileName) throws FileNotFoundException {
		return new FileInputStream(fileName);
	}

}
