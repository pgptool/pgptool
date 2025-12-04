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
package org.pgptool.gui.ui.decryptone;

import java.io.Serial;
import java.io.Serializable;
import org.summerb.utils.DtoBase;

/**
 * This class is used to reflect dialog parameters. It's used to help suggest user with processing
 * parameters based on input parameter(s)
 *
 * @author Sergey Karpushin
 */
public class DecryptionDialogParameters implements Serializable, DtoBase {
  @Serial private static final long serialVersionUID = 1090348970039260219L;

  private String sourceFile;

  private boolean useSameFolder;
  private boolean useTempFolder;
  private boolean inMemory;
  private String targetFile; // means user explicitly provided it

  private String decryptionKeyId;

  private boolean isDeleteSourceFile;
  private boolean isOpenTargetFolder;
  private boolean isOpenAssociatedApplication;

  private long createdAt;

  public String getSourceFile() {
    return sourceFile;
  }

  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }

  public boolean isUseSameFolder() {
    return useSameFolder;
  }

  public void setUseSameFolder(boolean useSameFolder) {
    this.useSameFolder = useSameFolder;
  }

  public String getTargetFile() {
    return targetFile;
  }

  public void setTargetFile(String targetFile) {
    this.targetFile = targetFile;
  }

  public String getDecryptionKeyId() {
    return decryptionKeyId;
  }

  public void setDecryptionKeyId(String decryptionKeyId) {
    this.decryptionKeyId = decryptionKeyId;
  }

  public boolean isDeleteSourceFile() {
    return isDeleteSourceFile;
  }

  public void setDeleteSourceFile(boolean isDeleteSourceFile) {
    this.isDeleteSourceFile = isDeleteSourceFile;
  }

  public boolean isOpenTargetFolder() {
    return isOpenTargetFolder;
  }

  public void setOpenTargetFolder(boolean isOpenTargetFolder) {
    this.isOpenTargetFolder = isOpenTargetFolder;
  }

  public boolean isOpenAssociatedApplication() {
    return isOpenAssociatedApplication;
  }

  public void setOpenAssociatedApplication(boolean isOpenAssociatedApplication) {
    this.isOpenAssociatedApplication = isOpenAssociatedApplication;
  }

  public boolean isUseTempFolder() {
    return useTempFolder;
  }

  public void setUseTempFolder(boolean useTempFolder) {
    this.useTempFolder = useTempFolder;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }

  public boolean isInMemory() {
    return inMemory;
  }

  public void setInMemory(boolean inMemory) {
    this.inMemory = inMemory;
  }
}
