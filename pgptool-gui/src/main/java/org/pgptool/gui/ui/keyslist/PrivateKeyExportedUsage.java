package org.pgptool.gui.ui.keyslist;

import java.io.Serializable;

public class PrivateKeyExportedUsage implements Serializable {
  private static final long serialVersionUID = -1306381502922185270L;
  private String keyId;
  private String targetFile;

  public PrivateKeyExportedUsage() {}

  public PrivateKeyExportedUsage(String keyId, String targetFile) {
    this.keyId = keyId;
    this.targetFile = targetFile;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  public String getTargetFile() {
    return targetFile;
  }

  public void setTargetFile(String targetFile) {
    this.targetFile = targetFile;
  }
}
