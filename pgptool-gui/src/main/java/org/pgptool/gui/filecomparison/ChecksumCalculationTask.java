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
import java.io.File;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import org.apache.commons.io.FilenameUtils;
import org.pgptool.gui.bkgoperation.Progress;
import org.pgptool.gui.bkgoperation.Progress.Updater;
import org.pgptool.gui.bkgoperation.ProgressHandler;
import org.pgptool.gui.bkgoperation.ProgressHandlerNoOpImpl;
import org.pgptool.gui.bkgoperation.UserRequestedCancellationException;

public class ChecksumCalculationTask implements Callable<Fingerprint> {
  private static final int READ_BUF_SIZE = 4096;

  private final String filePathName;
  private final MessageDigest messageDigest;
  private ProgressHandler progressHandler;

  public ChecksumCalculationTask(String filePathName, MessageDigest messageDigest) {
    this(filePathName, messageDigest, null);
  }

  public ChecksumCalculationTask(
      String filePathName, MessageDigest messageDigest, ProgressHandler optionalProgressHandler) {
    this.filePathName = filePathName;
    this.messageDigest = messageDigest;
    this.progressHandler = optionalProgressHandler;
    if (progressHandler == null) {
      progressHandler = new ProgressHandlerNoOpImpl();
    }
  }

  @Override
  public Fingerprint call() throws Exception {
    byte[] buf = new byte[READ_BUF_SIZE];
    CompletableFuture<Fingerprint> future = new CompletableFuture<>();

    Updater progress = Progress.create("encrBackAll.CheckingForChanges", progressHandler);
    progress.updateStepInfo(
        "encrBackAll.CheckingForChangesFile", FilenameUtils.getName(filePathName));
    progress.updateTotalSteps(BigInteger.valueOf(new File(filePathName).length()));

    try (InputStream is = new ChecksumCalcInputStream(messageDigest, filePathName, future)) {
      int readCount = 0;
      int totalReadCount = 0;
      while ((readCount = is.read(buf)) > 0) {
        totalReadCount += readCount;
        // continue reading
        if (progress.isCancellationRequested()) {
          throw new UserRequestedCancellationException();
        }
        progress.updateStepsTaken(BigInteger.valueOf(totalReadCount));
      }
    }
    Preconditions.checkState(future.isDone(), "Fingerprint should be available by now");
    return future.get();
  }
}
