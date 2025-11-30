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
package org.pgptool.gui.encryption.implpgp;

import java.io.File;
import org.apache.commons.io.FilenameUtils;

public class SourceInfo {
  private String name;
  private long size;
  private long modifiedAt;

  public SourceInfo(String name, long size, long modifiedAt) {
    super();
    this.name = name;
    this.size = size;
    this.modifiedAt = modifiedAt;
  }

  public static SourceInfo fromFile(String filePathName) {
    File file = new File(filePathName);
    return new SourceInfo(FilenameUtils.getName(filePathName), file.length(), file.lastModified());
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public long getModifiedAt() {
    return modifiedAt;
  }

  public void setModifiedAt(long modifiedAt) {
    this.modifiedAt = modifiedAt;
  }
}
