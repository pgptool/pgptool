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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import org.apache.log4j.Logger;

public class ChecksumCalcOutputStream extends FilterOutputStream {
  private static final Logger log = Logger.getLogger(ChecksumCalcOutputStream.class);

  private long size;
  private final MessageDigest messageDigest;
  private final String fileName;
  private boolean closed = false;
  private final CompletableFuture<Fingerprint> result;

  public ChecksumCalcOutputStream(
      MessageDigest messageDigest, String fileName, CompletableFuture<Fingerprint> result)
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
