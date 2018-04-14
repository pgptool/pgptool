package org.pgptool.gui.filecomparison;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;

public class ChecksumCalcInputStreamFactoryImpl implements ChecksumCalcInputStreamFactory {
	static Logger log = Logger.getLogger(ChecksumCalcInputStreamFactoryImpl.class);

	private MessageDigestFactory messageDigestFactory;
	private Map<String, Fingerprint> calculated = new HashMap<>();

	public ChecksumCalcInputStreamFactoryImpl(MessageDigestFactory messageDigestFactory) {
		this.messageDigestFactory = messageDigestFactory;
	}

	@Override
	public InputStream create(String fileName) throws FileNotFoundException {
		CompletableFuture<Fingerprint> result = new CompletableFuture<>();
		ChecksumCalcInputStream ret = new ChecksumCalcInputStream(messageDigestFactory.createNew(), fileName, result);
		result.thenAccept(x -> calculated.put(fileName, x));
		return ret;
	}

	@Override
	public Fingerprint getFingerprint(String fileName) {
		return calculated.get(fileName);
	}
}
