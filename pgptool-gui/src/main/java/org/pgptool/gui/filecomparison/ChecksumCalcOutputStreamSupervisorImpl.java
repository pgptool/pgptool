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

import com.google.common.base.Preconditions;
import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import org.apache.log4j.Logger;

public class ChecksumCalcOutputStreamSupervisorImpl implements ChecksumCalcOutputStreamSupervisor {
  static Logger log = Logger.getLogger(ChecksumCalcOutputStreamSupervisorImpl.class);

  private final MessageDigestFactory messageDigestFactory;
  private Fingerprint fingerprint;
  private CompletableFuture<Fingerprint> fingerprintFuture;
  private String fileName;

  public ChecksumCalcOutputStreamSupervisorImpl(MessageDigestFactory messageDigestFactory) {
    this.messageDigestFactory = messageDigestFactory;
  }

  @Override
  public OutputStream get(String fileName) throws FileNotFoundException {
    Preconditions.checkState(
        fingerprintFuture == null,
        "This wrapper is done, you can't use it anymore. CLone it if you need another one");
    this.fileName = fileName;
    fingerprintFuture = new CompletableFuture<>();
    ChecksumCalcOutputStream ret =
        new ChecksumCalcOutputStream(messageDigestFactory.createNew(), fileName, fingerprintFuture);
    fingerprintFuture.thenAccept(x -> fingerprint = x);
    return ret;
  }

  @Override
  public Fingerprint getFingerprint() {
    Preconditions.checkArgument(
        fingerprint != null, "Fingerprint wasn't calculated yet: %s", fileName);
    return fingerprint;
  }

  @Override
  public ChecksumCalcOutputStreamSupervisor clone() {
    return new ChecksumCalcOutputStreamSupervisorImpl(messageDigestFactory);
  }
}
