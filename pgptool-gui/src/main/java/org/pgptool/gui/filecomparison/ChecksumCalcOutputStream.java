package org.pgptool.gui.filecomparison;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;

public class ChecksumCalcOutputStream extends FilterOutputStream {
	private static Logger log = Logger.getLogger(ChecksumCalcOutputStream.class);

	private long size;
	private MessageDigest messageDigest;
	private String fileName;
	private boolean closed = false;
	private CompletableFuture<Fingerprint> result;

	public ChecksumCalcOutputStream(MessageDigest messageDigest, String fileName, CompletableFuture<Fingerprint> result)
			throws FileNotFoundException {
		super(new FileOutputStream(fileName));
		this.messageDigest = messageDigest;
		this.fileName = fileName;
		this.result = result;
		
		log.debug("Opened for " + fileName);
	}

	/** Returns the number of bytes written. */
	public long getSize() {
		return size;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		out.write(b, off, len);
		messageDigest.update(b, off, len);
		size += len;
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
		messageDigest.update((byte) b);
		size++;
	}

	// Overriding close() because FilterOutputStream's close() method pre-JDK8 has
	// bad behavior:
	// it silently ignores any exception thrown by flush(). Instead, just close the
	// delegate stream.
	// It should flush itself if necessary.
	@Override
	public void close() throws IOException {
		out.close();

		if (closed) {
			return;
		}
		closed = true;

		log.debug("Closed for " + fileName);

		Fingerprint fingerprint = new Fingerprint();
		fingerprint.setSize(size);
		byte[] encoded = Base64.getEncoder().encode(messageDigest.digest());
		fingerprint.setChecksum(new String(encoded, "UTF-8"));
		log.debug("File " + fileName + " fingerprint: " + fingerprint);
		result.complete(fingerprint);
	}
}