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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChecksumCalcInputStream extends FilterInputStream {
  private static final Logger log = LoggerFactory.getLogger(ChecksumCalcInputStream.class);

  private long size;
  private long mark = -1;
  private MessageDigest messageDigestMark;

  private MessageDigest messageDigest;
  private final String fileName;
  private boolean closed = false;
  private final CompletableFuture<Fingerprint> reportTo;

  public ChecksumCalcInputStream(
      MessageDigest messageDigest, String fileName, CompletableFuture<Fingerprint> reportTo)
      throws FileNotFoundException {
    super(new FileInputStream(fileName));
    this.messageDigest = messageDigest;
    this.fileName = fileName;
    this.reportTo = reportTo;
    log.debug("Opened for {}", fileName);
  }

  /** Returns the number of bytes written. */
  public long getSize() {
    return size;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    int result = in.read(b, off, len);
    if (result != -1) {
      size += result;
      messageDigest.update(b, off, result);
    }
    return result;
  }

  @Override
  public int read() throws IOException {
    int result = in.read();
    if (result != -1) {
      size++;
      messageDigest.update((byte) result);
    }
    return result;
  }

  @Override
  public long skip(long n) throws IOException {
    throw new IOException("Seek is not supported");
  }

  @Override
  public synchronized void mark(int readlimit) {
    in.mark(readlimit);
    mark = size;
    try {
      messageDigestMark = (MessageDigest) messageDigest.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("Mark operation is not supported", e);
    }
  }

  @Override
  public synchronized void reset() throws IOException {
    if (!in.markSupported()) {
      throw new IOException("Mark not supported");
    }
    if (mark == -1) {
      throw new IOException("Mark not set");
    }

    in.reset();
    messageDigest = messageDigestMark;
    size = mark;
  }

  // Overriding close() because FilterOutputStream's close() method pre-JDK8 has
  // bad behavior:
  // it silently ignores any exception thrown by flush(). Instead, just close the
  // delegate stream.
  // It should flush itself if necessary.
  @Override
  public void close() throws IOException {
    in.close();

    if (closed) {
      return;
    }
    closed = true;

    log.debug("Closed for {}", fileName);

    Fingerprint fingerprint = new Fingerprint();
    fingerprint.setSize(size);
    byte[] encoded = Base64.getEncoder().encode(messageDigest.digest());
    fingerprint.setChecksum(new String(encoded, StandardCharsets.UTF_8));
    log.debug("File {} fingerprint: {}", fileName, fingerprint);
    reportTo.complete(fingerprint);
  }
}
