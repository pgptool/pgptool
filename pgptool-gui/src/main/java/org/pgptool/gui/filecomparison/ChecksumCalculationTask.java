package org.pgptool.gui.filecomparison;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Preconditions;

public class ChecksumCalculationTask implements Callable<Fingerprint> {
	private static final int READ_BUF_SIZE = 4096;

	private String filePathName;
	private MessageDigest messageDigest;

	public ChecksumCalculationTask(String filePathName, MessageDigest messageDigest) {
		this.filePathName = filePathName;
		this.messageDigest = messageDigest;
	}

	@Override
	public Fingerprint call() throws Exception {
		byte[] buf = new byte[READ_BUF_SIZE];
		CompletableFuture<Fingerprint> future = new CompletableFuture<>();
		try (InputStream is = new ChecksumCalcInputStream(messageDigest, filePathName, future)) {
			while (is.read(buf) > 0) {
				// continue reading
			}
		}
		Preconditions.checkState(future.isDone(), "Fingerprint should be available by now");
		return future.get();
	}
}
