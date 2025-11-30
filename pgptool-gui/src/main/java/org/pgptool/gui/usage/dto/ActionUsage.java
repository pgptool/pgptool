package org.pgptool.gui.usage.dto;

import java.io.Serial;
import java.io.Serializable;

public class ActionUsage implements Serializable {
  @Serial private static final long serialVersionUID = -6864705568804533564L;
  private String ctx;
  private String action;

  public ActionUsage() {}

  public ActionUsage(String context, String actionName) {
    this.ctx = context;
    this.action = actionName;
  }

  public String getCtx() {
    return ctx;
  }

  public void setCtx(String context) {
    this.ctx = context;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String actionName) {
    this.action = actionName;
  }
}
