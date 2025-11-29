package org.pgptool.gui.usage.dto;

import java.io.Serializable;

public class DecryptedTextUsage implements Serializable {
  private static final long serialVersionUID = -1912043362266429159L;
  private String decryptionKeyId;

  public DecryptedTextUsage() {}

  public DecryptedTextUsage(String decryptionKeyId) {
    this.decryptionKeyId = decryptionKeyId;
  }

  public String getDecryptionKeyId() {
    return decryptionKeyId;
  }

  public void setDecryptionKeyId(String decryptionKeyId) {
    this.decryptionKeyId = decryptionKeyId;
  }
}
