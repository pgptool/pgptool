/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
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
