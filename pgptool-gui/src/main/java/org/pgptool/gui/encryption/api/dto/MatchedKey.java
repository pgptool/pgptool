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

/**
 * This DTO was created to clarify what requestedKey id was used to match key from a keyring
 *
 * @author Sergey Karpushin
 * @param
 */
public class MatchedKey {
  private String requestedKeyId;
  private Key matchedKey;

  /**
   * @param requestedKeyId this key id found in encrypted file
   * @param matchedKey this key was found as a matched key, the one that contains requested
   *     decryption key
   */
  public MatchedKey(String requestedKeyId, Key matchedKey) {
    super();
    this.requestedKeyId = requestedKeyId;
    this.matchedKey = matchedKey;
  }

  public String getRequestedKeyId() {
    return requestedKeyId;
  }

  public void setRequestedKeyId(String requestedKeyId) {
    this.requestedKeyId = requestedKeyId;
  }

  public Key getMatchedKey() {
    return matchedKey;
  }

  public void setMatchedKey(Key matchedKey) {
    this.matchedKey = matchedKey;
  }
}
