package org.pgptool.gui.filecomparison;

import org.pgptool.gui.encryption.api.OutputStreamSupervisor;
import org.summerb.utils.Clonnable;

/**
 * This factory will yield streams which will calculate checksum during
 * operation. This way there will be no need to go through stream again
 * 
 * Single instance of this factory can be used to create only 1 stream. If you
 * need to create another one -- clone it
 * 
 * @author sergeyk
 *
 */
public interface ChecksumCalcOutputStreamSupervisor
		extends OutputStreamSupervisor, Clonnable<ChecksumCalcOutputStreamSupervisor> {
	Fingerprint getFingerprint();
}
