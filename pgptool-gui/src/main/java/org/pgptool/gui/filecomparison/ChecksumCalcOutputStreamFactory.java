package org.pgptool.gui.filecomparison;

import org.pgptool.gui.encryption.api.OutputStreamFactory;

/**
 * This factory will yield streams which will calculate checksum during
 * operation. This way there will be no need to go through stream again
 * 
 * @author sergeyk
 *
 */
public interface ChecksumCalcOutputStreamFactory extends OutputStreamFactory {
	Fingerprint getFingerprint(String outputFileName);
}
