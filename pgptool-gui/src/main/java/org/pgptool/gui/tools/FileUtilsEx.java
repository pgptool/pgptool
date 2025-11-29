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
package org.pgptool.gui.tools;

import com.google.common.base.Preconditions;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.bkgoperation.UserRequestedCancellationException;
import org.springframework.util.StringUtils;

public class FileUtilsEx {
  private static Logger log = Logger.getLogger(FileUtilsEx.class);

  /** bruteforce filename adding index to base filename until vacant filename found. */
  public static String ensureFileNameVacant(String requestedTargetFile) {
    String ret = requestedTargetFile;
    int idx = 0;
    String basePathName =
        FilenameUtils.getFullPath(requestedTargetFile)
            + FilenameUtils.getBaseName(requestedTargetFile);
    String ext = FilenameUtils.getExtension(requestedTargetFile);
    while (new File(ret).exists()) {
      idx++;
      ret = basePathName + "-" + idx;
      if (StringUtils.hasText(ext)) {
        ret += "." + ext;
      }
    }
    return ret;
  }

  /**
   * This method will pick OTHER vacant file name to write data to and when operation is completed,
   * old file will be removed and new file will be renamed to target name.
   *
   * <p>See https://github.com/pgptool/pgptool/issues/131 "Improvement: when encrypting back - don't
   * overwrite existing file until encryption 100% completed"
   *
   * @param targetFile desired target file name
   * @param fileCreatorLogic operation which will create the file
   */
  public static void baitAndSwitch(String targetFile, FileCreatorLogic fileCreatorLogic)
      throws Exception, UserRequestedCancellationException {

    File targetFileFile = new File(targetFile);
    File targetFolder = targetFileFile.getParentFile();
    if (!targetFolder.exists()) {
      Preconditions.checkState(
          targetFolder.mkdirs(), "Failed to ensure parent directories for %s", targetFile);
      log.debug("Had to create parent directory for " + targetFile);
    }

    boolean needBaitAndSwitch = targetFileFile.exists();
    if (!needBaitAndSwitch) {
      fileCreatorLogic.createFile(targetFile);
      return;
    }

    String tempTargetFile = FileUtilsEx.ensureFileNameVacant(targetFile);
    File tempTargetFileFile = new File(tempTargetFile);
    boolean newMoved = false;
    boolean originalDeleted = false;
    try {
      fileCreatorLogic.createFile(tempTargetFile);

      FileUtils.forceDelete(targetFileFile);
      originalDeleted = true;
      FileUtils.moveFile(tempTargetFileFile, targetFileFile);
      newMoved = true;
    } catch (Throwable t) {
      if (originalDeleted && !newMoved) {
        throw new GenericException("exception.tempFileSwitchFailed", t, tempTargetFile, targetFile);
      }
      throw t;
    }
  }

  public static interface FileCreatorLogic {
    void createFile(String fileName) throws Exception, UserRequestedCancellationException;
  }
}
