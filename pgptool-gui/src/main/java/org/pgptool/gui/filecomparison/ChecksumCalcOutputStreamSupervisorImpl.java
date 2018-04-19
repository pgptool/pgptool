package org.pgptool.gui.filecomparison;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

public class ChecksumCalcOutputStreamSupervisorImpl implements ChecksumCalcOutputStreamSupervisor {
	static Logger log = Logger.getLogger(ChecksumCalcOutputStreamSupervisorImpl.class);

	private MessageDigestFactory messageDigestFactory;
	private Fingerprint fingerprint;
	private CompletableFuture<Fingerprint> fingerprintFuture;
	private String fileName;

	public ChecksumCalcOutputStreamSupervisorImpl(MessageDigestFactory messageDigestFactory) {
		this.messageDigestFactory = messageDigestFactory;
	}

	@Override
	public OutputStream get(String fileName) throws FileNotFoundException {
		Preconditions.checkState(fingerprintFuture == null,
				"This wrapper is done, you can't use it anymore. CLone it if you need another one");
		this.fileName = fileName;
		fingerprintFuture = new CompletableFuture<>();
		ChecksumCalcOutputStream ret = new ChecksumCalcOutputStream(messageDigestFactory.createNew(), fileName,
				fingerprintFuture);
		fingerprintFuture.thenAccept(x -> fingerprint = x);
		return ret;
	}

	@Override
	public Fingerprint getFingerprint() {
		Preconditions.checkArgument(fingerprint != null, "Fingerprint wasn't calculated yet: %s", fileName);
		return fingerprint;
	}

	@Override
	public ChecksumCalcOutputStreamSupervisor clone() {
		return new ChecksumCalcOutputStreamSupervisorImpl(messageDigestFactory);
	}
}
