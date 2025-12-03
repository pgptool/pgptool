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
package org.pgptool.gui.encryption.api;

import org.pgptool.gui.encryption.api.dto.ChangePasswordParams;
import org.pgptool.gui.encryption.api.dto.ChangeUserIdParams;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;

public interface KeyGeneratorService {
  /**
   * Create key
   *
   * @param emptyPassphraseConsent must be explicitly specified if user agreed to use empty
   *     passphrase
   */
  Key createNewKey(CreateKeyParams params, boolean emptyPassphraseConsent);

  Key changeKeyPassword(Key key, ChangePasswordParams params, boolean emptyPasswordConsent);

  /**
   * Add a new primary User ID (Full name and Email) to the key, re-certifying it with the key's
   * master secret key. Existing UIDs are kept, but marked as non-primary.
   */
  Key replacePrimaryUserId(Key key, ChangeUserIdParams params);
}
