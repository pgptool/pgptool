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
package org.pgptool.gui.config.api.exceptions;

import java.io.Serial;
import org.summerb.i18n.HasMessageArgs;
import org.summerb.i18n.HasMessageCode;

public class ConfigWriteException extends Exception implements HasMessageCode, HasMessageArgs {
  @Serial private static final long serialVersionUID = 1253040402059386002L;

  private final String propertyFileLocation;

  public ConfigWriteException(
      String propertyFileLocation, String technicalMessage, Throwable cause) {
    super(technicalMessage, cause);
    this.propertyFileLocation = propertyFileLocation;
  }

  @Override
  public String getMessageCode() {
    return "exceptions.unexpected.failedToSaveConfiguration";
  }

  @Override
  public Object[] getMessageArgs() {
    return new Object[] {propertyFileLocation};
  }
}
