package org.pgptool.gui.encryption.api;

import java.io.FileNotFoundException;
import java.io.OutputStream;

/**
 * This interface will return stream for given file. It can only create 1 stream
 * for 1 file, subsequent calls will fail
 * 
 * @author sergeyk
 *
 */
public interface OutputStreamSupervisor {

	OutputStream get(String fileName) throws FileNotFoundException;

}
