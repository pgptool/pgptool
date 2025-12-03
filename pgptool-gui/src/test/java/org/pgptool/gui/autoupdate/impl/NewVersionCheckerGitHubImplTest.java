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
package org.pgptool.gui.autoupdate.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.pgptool.gui.autoupdate.api.UpdatePackageInfo;

public class NewVersionCheckerGitHubImplTest {

  @Test
  public void testFindNewUpdateIfAvailable() throws Exception {
    NewVersionCheckerGitHubImpl f = new NewVersionCheckerGitHubImpl();
    f.setAppVersion("0.0.0.1");

    UpdatePackageInfo result = f.findNewUpdateIfAvailable();
    assertNotNull(result);
  }

  @Test
  public void testGetCurrentVersion_ExpectNullByDefault() {
    NewVersionCheckerGitHubImpl f = new NewVersionCheckerGitHubImpl();
    assertEquals(NewVersionCheckerGitHubImpl.VERSION_UNRESOLVED, f.getCurrentVersion());
  }

  @Test
  public void testGetCurrentVersion_ExpectHardcoded() {
    NewVersionCheckerGitHubImpl f = new NewVersionCheckerGitHubImpl();
    f.setAppVersion("0.0.0.0");
    assertEquals("0.0.0.0", f.getCurrentVersion());
  }
}
