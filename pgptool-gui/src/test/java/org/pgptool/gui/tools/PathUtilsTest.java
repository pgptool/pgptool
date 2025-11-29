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

import static org.junit.Assert.assertEquals;

import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

public class PathUtilsTest {

  @Test
  public void testExtractBasePath() {
    // NOTE: Nut sure how to write one test which will work on both
    // platforms =(
    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
      String result = FilenameUtils.getFullPathNoEndSeparator("c:\\temp\\aaa.docx");
      assertEquals("c:\\temp", result);
    } else {
      String result = FilenameUtils.getFullPathNoEndSeparator("/var/opt/file.txt");
      assertEquals("/var/opt", result);
    }
  }
}
