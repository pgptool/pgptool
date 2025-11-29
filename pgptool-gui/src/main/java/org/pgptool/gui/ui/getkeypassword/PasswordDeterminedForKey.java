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
package org.pgptool.gui.ui.getkeypassword;

import org.pgptool.gui.encryption.api.dto.Key;

public class PasswordDeterminedForKey {
  private String decryptionKeyId;
  private Key matchedKey;
  private String password;

  public PasswordDeterminedForKey(String decryptionKeyId, Key matchedKey, String password) {
    super();
    this.decryptionKeyId = decryptionKeyId;
    this.matchedKey = matchedKey;
    this.password = password;
  }

  public Key getMatchedKey() {
    return matchedKey;
  }

  public void setMatchedKey(Key key) {
    this.matchedKey = key;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getDecryptionKeyId() {
    return decryptionKeyId;
  }

  public void setDecryptionKeyId(String decryptionKeyId) {
    this.decryptionKeyId = decryptionKeyId;
  }
}
