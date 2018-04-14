package org.pgptool.gui.filecomparison;

import java.security.MessageDigest;

/**
 * This interface is involved in the process of calculation of
 * {@link Fingerprint}. It's used in files comaprison process. See
 * https://github.com/pgptool/pgptool/issues/117
 * 
 * @author sergeyk
 *
 */
public interface MessageDigestFactory {
	MessageDigest createNew();
}
