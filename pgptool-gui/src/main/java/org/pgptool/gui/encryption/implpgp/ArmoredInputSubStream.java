package org.pgptool.gui.encryption.implpgp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ArmoredInputSubStream extends InputStream {
	private static final String CHARSET = "UTF-8";
	private static final String BLOCK_BEGIN = "-----BEGIN";
	private static final String BLOCK_END = "-----END";

	private InputStream inputStream;
	private String pending;
	private byte[] pendingBytes;
	private int pendingPos;
	private BufferedReader reader;
	private boolean lastLine;

	public ArmoredInputSubStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public boolean hasNextSubStream() {
		if (inputStream == null) {
			return false;
		}

		try {
			if (reader == null) {
				reader = new BufferedReader(new InputStreamReader(inputStream));
			}

			while (true) {
				pending = reader.readLine();
				if (pending == null) {
					inputStream = null;
					return false;
				}
				pending += "\n";
				pendingBytes = pending.getBytes(CHARSET);
				pendingPos = 0;
				lastLine = false;
				if (pending.startsWith(BLOCK_BEGIN)) {
					return true;
				}
			}
		} catch (Throwable t) {
			throw new RuntimeException("Failed to determine if there is next stream available", t);
		}
	}

	@Override
	public int read() throws IOException {
		while (true) {
			if (pendingPos < pendingBytes.length) {
				int retPos = pendingPos;
				pendingPos++;
				return pendingBytes[retPos];
			}

			if (lastLine) {
				return -1;
			}

			pending = reader.readLine();
			if (pending == null) {
				return -1;
			}
			pending += "\n";
			pendingBytes = pending.getBytes(CHARSET);
			pendingPos = 0;
			if (pending.startsWith(BLOCK_END)) {
				lastLine = true;
			}
			continue;
		}
	}
}
