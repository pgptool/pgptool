package org.pgptool.gui.usage.dto;

import java.io.Serializable;
import org.pgptool.gui.ui.encryptone.EncryptionDialogParameters;

public class EncryptBackAllIterationUsage implements Serializable {
  private static final long serialVersionUID = 5640310966571733889L;
  private EncryptionDialogParameters encryptionParams;

  public EncryptBackAllIterationUsage() {}

  public EncryptBackAllIterationUsage(EncryptionDialogParameters encryptionParams) {
    this.encryptionParams = encryptionParams;
  }

  public EncryptionDialogParameters getEncryptionParams() {
    return encryptionParams;
  }

  public void setEncryptionParams(EncryptionDialogParameters encryptionParams) {
    this.encryptionParams = encryptionParams;
  }
}
