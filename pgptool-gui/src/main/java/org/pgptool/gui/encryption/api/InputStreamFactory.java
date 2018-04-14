package org.pgptool.gui.encryption.api;

import java.io.FileNotFoundException;
import java.io.InputStream;

public interface InputStreamFactory {

	InputStream create(String fileName) throws FileNotFoundException;

}
