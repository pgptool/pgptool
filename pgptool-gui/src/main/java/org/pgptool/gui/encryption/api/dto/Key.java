/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.pgptool.gui.encryption.api.dto;

import org.summerb.utils.DtoBase;

public class Key implements DtoBase {
  private static final long serialVersionUID = 1614562515516152578L;

  /**
   * This field contains some parts of parsed data. It's intended for read-only use, do not change
   * it manually
   */
  private KeyInfo keyInfo;

  private KeyData keyData;

  public KeyInfo getKeyInfo() {
    return keyInfo;
  }

  /**
   * @deprecated for IO purposes only, do not modify manually
   */
  @Deprecated
  public void setKeyInfo(KeyInfo keyInfo) {
    this.keyInfo = keyInfo;
  }

  public KeyData getKeyData() {
    return keyData;
  }

  public void setKeyData(KeyData keyData) {
    this.keyData = keyData;
  }

  @Override
  public String toString() {
    if (keyInfo == null || keyInfo.getUser() == null) {
      return super.toString();
    }

    return keyInfo.getUser();
  }

  public static boolean isSameKeyId(Key o1, Key o2) {
    if (o1 == null || o2 == null) {
      return false;
    }
    if (o1.getKeyInfo() == null || o2.getKeyInfo() == null) {
      return false;
    }
    if (o1.getKeyInfo().getKeyId() == null || o2.getKeyInfo().getKeyId() == null) {
      return false;
    }

    return o1.getKeyInfo().getKeyId().equals(o2.getKeyInfo().getKeyId());
  }
}
