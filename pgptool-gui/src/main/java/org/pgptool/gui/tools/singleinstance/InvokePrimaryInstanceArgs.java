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
package org.pgptool.gui.tools.singleinstance;

import java.io.Serial;
import org.summerb.utils.DtoBase;

public class InvokePrimaryInstanceArgs implements DtoBase {
  @Serial private static final long serialVersionUID = -1935402056245763044L;

  private String[] commandLineArgs;

  public InvokePrimaryInstanceArgs(String[] commandLineArgs) {
    this.commandLineArgs = commandLineArgs;
  }

  public InvokePrimaryInstanceArgs() {}

  public String[] getCommandLineArgs() {
    return commandLineArgs;
  }

  public void setCommandLineArgs(String[] commandLineArgs) {
    this.commandLineArgs = commandLineArgs;
  }
}
