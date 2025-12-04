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

import com.google.common.base.Preconditions;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class OutputStreamSupervisorImpl implements OutputStreamSupervisor {
  private OutputStream ret;

  @Override
  public OutputStream get(String fileName) throws FileNotFoundException {
    return get(new FileOutputStream(fileName, false));
  }

  @Override
  public OutputStream get(OutputStream target) {
    Preconditions.checkState(ret == null, "Stream was already returned");
    ret = target;
    return ret;
  }
}
