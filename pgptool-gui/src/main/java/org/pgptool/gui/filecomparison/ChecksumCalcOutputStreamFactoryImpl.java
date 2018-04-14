package org.pgptool.gui.filecomparison;

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;

import com.google.common.base.Preconditions;

public class ChecksumCalcOutputStreamFactoryImpl implements ChecksumCalcOutputStreamFactory {
	static Logger log = Logger.getLogger(ChecksumCalcOutputStreamFactoryImpl.class);

	private MessageDigestFactory messageDigestFactory;
	private Map<String, Fingerprint> calculated = new HashMap<>();

	public ChecksumCalcOutputStreamFactoryImpl(MessageDigestFactory messageDigestFactory) {
		this.messageDigestFactory = messageDigestFactory;
	}

	@Override
	public OutputStream create(String fileName) throws FileNotFoundException {
		// return new ChecksumCalcOutputStream(messageDigestFactory.createNew(),
		// fileName);
		CompletableFuture<Fingerprint> result = new CompletableFuture<>();
		ChecksumCalcOutputStream ret = new ChecksumCalcOutputStream(messageDigestFactory.createNew(), fileName, result);
		result.thenAccept(x -> calculated.put(fileName, x));
		return ret;
	}

	@Override
	public Fingerprint getFingerprint(String outputFileName) {
		Fingerprint ret = calculated.get(outputFileName);
		Preconditions.checkArgument(ret != null, "This ilfe was never calculated: %s", outputFileName);
		return ret;
	}
}
