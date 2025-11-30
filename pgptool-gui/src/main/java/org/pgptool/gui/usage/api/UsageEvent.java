package org.pgptool.gui.usage.api;

import java.io.Serial;
import java.io.Serializable;
import org.summerb.utils.DtoBase;

public class UsageEvent implements DtoBase {
  @Serial private static final long serialVersionUID = -2469884370085267887L;

  /** Event parameters */
  private Serializable p;

  /**
   * @deprecated do not use this manually. Empty constructor is only for IO purposes
   */
  @Deprecated
  public UsageEvent() {}

  public UsageEvent(Serializable parameters) {
    super();
    this.p = parameters;
  }

  public Serializable getP() {
    return p;
  }

  public void setP(Serializable parameters) {
    this.p = parameters;
  }
}
