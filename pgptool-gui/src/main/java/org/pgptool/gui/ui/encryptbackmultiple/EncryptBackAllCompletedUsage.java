package org.pgptool.gui.ui.encryptbackmultiple;

import java.io.Serializable;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptBackMultiplePm.BatchEncryptionResult;

public class EncryptBackAllCompletedUsage implements Serializable {
  private static final long serialVersionUID = -3788024643497033395L;
  private BatchEncryptionResult ret;

  public EncryptBackAllCompletedUsage() {}

  public EncryptBackAllCompletedUsage(BatchEncryptionResult ret) {
    this.ret = ret;
  }

  public BatchEncryptionResult getRet() {
    return ret;
  }

  public void setRet(BatchEncryptionResult ret) {
    this.ret = ret;
  }
}
