package org.pgptool.gui.encryption.api;

import java.io.FileNotFoundException;
import java.io.OutputStream;

public interface OutputStreamFactory {

	OutputStream create(String fileName) throws FileNotFoundException;

}
