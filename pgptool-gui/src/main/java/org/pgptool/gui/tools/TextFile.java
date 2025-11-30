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
package org.pgptool.gui.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Scanner;

public class TextFile {
  private static final String DEFAULT_ENCODING = "UTF-8";

  public static String read(String fileName) throws Exception {
    StringBuilder text = new StringBuilder();
    String NL = System.lineSeparator();
    try (Scanner scanner = new Scanner(new FileInputStream(fileName), DEFAULT_ENCODING)) {
      while (scanner.hasNextLine()) {
        text.append(scanner.nextLine()).append(NL);
      }
    }
    return text.toString();
  }

  public static void write(String fileName, String configContents) throws Exception {
    try (Writer out = new OutputStreamWriter(new FileOutputStream(fileName), DEFAULT_ENCODING)) {
      out.write(configContents);
    }
  }
}
