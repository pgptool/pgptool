package org.pgptool.gui.usage.dto;

import java.io.Serial;
import java.io.Serializable;

public class EncryptBackAllUsage implements Serializable {
  @Serial private static final long serialVersionUID = 5212346572766253072L;
  private int totalFiles;

  public EncryptBackAllUsage() {}

  public EncryptBackAllUsage(int totalFiles) {
    this.totalFiles = totalFiles;
  }

  public int getTotalFiles() {
    return totalFiles;
  }

  public void setTotalFiles(int totalFiles) {
    this.totalFiles = totalFiles;
  }
}
