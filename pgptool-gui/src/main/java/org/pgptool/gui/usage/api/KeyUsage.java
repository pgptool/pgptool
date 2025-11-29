package org.pgptool.gui.usage.api;

import java.io.Serializable;

public class KeyUsage implements Serializable {
  private String keyId;

  private static final long serialVersionUID = 8992056079418764202L;

  public KeyUsage(String requestedKeyId) {
    this.keyId = requestedKeyId;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String requestedKeyId) {
    this.keyId = requestedKeyId;
  }
}
