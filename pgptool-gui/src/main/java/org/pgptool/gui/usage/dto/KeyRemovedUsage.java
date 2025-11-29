package org.pgptool.gui.usage.dto;

import java.io.Serializable;

public class KeyRemovedUsage implements Serializable {
  private String keyId;

  private static final long serialVersionUID = -6133522576192983429L;

  public KeyRemovedUsage(String keyId) {
    this.keyId = keyId;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }
}
