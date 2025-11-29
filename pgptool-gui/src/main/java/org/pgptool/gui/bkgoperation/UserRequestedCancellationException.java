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
package org.pgptool.gui.bkgoperation;

import org.summerb.i18n.HasMessageCode;

/**
 * Thrown when operation canceled due to cancellation requested by user
 *
 * @author sergeyk
 */
public class UserRequestedCancellationException extends Exception implements HasMessageCode {
  private static final long serialVersionUID = 1290562826825044541L;

  private final boolean softCancel;

  public UserRequestedCancellationException() {
    super("Operation canceled by user");
    softCancel = false;
  }

  public UserRequestedCancellationException(boolean softCancel) {
    super("Operation canceled by user");
    this.softCancel = softCancel;
  }

  @Override
  public String getMessageCode() {
    return "exception.userRequestedCancellation";
  }

  public boolean isSoftCancel() {
    return softCancel;
  }
}
