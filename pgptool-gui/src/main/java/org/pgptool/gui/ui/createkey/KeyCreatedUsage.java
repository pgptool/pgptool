package org.pgptool.gui.ui.createkey;

import java.io.Serial;
import java.io.Serializable;
import org.pgptool.gui.encryption.api.dto.KeyInfo;

public class KeyCreatedUsage implements Serializable {
  @Serial private static final long serialVersionUID = -8971219471059015750L;

  private KeyInfo keyInfo;

  public KeyCreatedUsage() {}

  public KeyCreatedUsage(KeyInfo keyInfo) {
    this.keyInfo = keyInfo;
  }

  public KeyInfo getKeyInfo() {
    return keyInfo;
  }

  public void setKeyInfo(KeyInfo keyInfo) {
    this.keyInfo = keyInfo;
  }
}
