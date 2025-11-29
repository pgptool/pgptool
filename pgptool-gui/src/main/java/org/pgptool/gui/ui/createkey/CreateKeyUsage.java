package org.pgptool.gui.ui.createkey;

import java.io.Serializable;

public class CreateKeyUsage implements Serializable {
  private static final long serialVersionUID = -6652489944057680318L;
  private String userName;
  private String email;

  public CreateKeyUsage() {}

  public CreateKeyUsage(String userName, String email) {
    this.userName = userName;
    this.email = email;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
