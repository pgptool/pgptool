package org.pgpvault.gui.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.Logger;

public class IoStreamUtils {
	private static Logger log = Logger.getLogger(IoStreamUtils.class);

	public static void safeClose(InputStream stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (IOException t) {
			log.debug("Failed to close the stream", t);
		} catch (Throwable t) {
			throw new RuntimeException("Something bad hapened while trying to close the stream", t);
		}
	}

	public static void safeClose(OutputStream stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (IOException t) {
			log.debug("Failed to close the stream", t);
		} catch (Throwable t) {
			throw new RuntimeException("Something bad hapened while trying to close the stream", t);
		}
	}
}
