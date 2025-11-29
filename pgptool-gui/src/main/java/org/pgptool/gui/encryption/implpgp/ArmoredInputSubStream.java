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
package org.pgptool.gui.encryption.implpgp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.pgptool.gui.app.GenericException;

public class ArmoredInputSubStream extends InputStream {
  private static final String CHARSET = "UTF-8";
  private static final String BLOCK_BEGIN = "-----BEGIN";
  private static final String BLOCK_END = "-----END";

  private InputStream inputStream;
  private String pending;
  private byte[] pendingBytes;
  private int pendingPos;
  private BufferedReader reader;
  private boolean lastLine;

  public ArmoredInputSubStream(InputStream inputStream) {
    this.inputStream = inputStream;
  }

  public boolean hasNextSubStream() throws GenericException {
    if (inputStream == null) {
      return false;
    }

    try {
      if (reader == null) {
        reader = new BufferedReader(new InputStreamReader(inputStream));
      }

      while (true) {
        pending = reader.readLine();
        if (pending == null) {
          inputStream = null;
          return false;
        }
        pending += "\n";
        pendingBytes = pending.getBytes(CHARSET);
        pendingPos = 0;
        lastLine = false;
        if (pending.startsWith(BLOCK_BEGIN)) {
          return true;
        }
      }
    } catch (Throwable t) {
      throw new GenericException("warning.couldNotFindPgpBlock", t);
    }
  }

  @Override
  public int read() throws IOException {
    while (true) {
      if (pendingPos < pendingBytes.length) {
        int retPos = pendingPos;
        pendingPos++;
        return pendingBytes[retPos];
      }

      if (lastLine) {
        return -1;
      }

      pending = reader.readLine();
      if (pending == null) {
        return -1;
      }
      pending += "\n";
      pendingBytes = pending.getBytes(CHARSET);
      pendingPos = 0;
      if (pending.startsWith(BLOCK_END)) {
        lastLine = true;
      }
      continue;
    }
  }
}
