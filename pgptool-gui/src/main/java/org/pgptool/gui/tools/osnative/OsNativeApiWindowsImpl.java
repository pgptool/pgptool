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
package org.pgptool.gui.tools.osnative;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sergey Karpushin
 */
public class OsNativeApiWindowsImpl implements OsNativeApi {
  private static final Logger log = LoggerFactory.getLogger(OsNativeApiWindowsImpl.class);

  private Kernel32 kernel32;
  private Shell32 shell32;

  /**
   * This method will try to solve issue when java executable cannot transfer argument in utf
   * encoding. cyrillic languages screws up and application receives ??????? instead of real text
   */
  @Override
  public String[] getCommandLineArguments(String[] fallBackTo) {
    try {
      if (fallBackTo == null || fallBackTo.length == 0) {
        return fallBackTo;
      }

      log.debug("In case we fail fallback would happen to: {}", Arrays.toString(fallBackTo));
      String[] nativeCommandLine = getFullCommandLine();
      log.debug(
          "According to Windows API, program was started with arguments: {}",
          Arrays.toString(nativeCommandLine));

      String[] ret = new String[fallBackTo.length];

      System.arraycopy(
          nativeCommandLine, nativeCommandLine.length - fallBackTo.length, ret, 0, ret.length);

      log.debug("These arguments will be used: {}", Arrays.toString(nativeCommandLine));
      return ret;
    } catch (Throwable t) {
      log.error("Failed to use JNA to get current program command line arguments", t);
      return fallBackTo;
    }
  }

  private String[] getFullCommandLine() {
    try {
      IntByReference argc = new IntByReference();
      Pointer argv_ptr = getShell32().CommandLineToArgvW(getKernel32().GetCommandLineW(), argc);
      String[] argv = argv_ptr.getWideStringArray(0, argc.getValue());
      getKernel32().LocalFree(argv_ptr);
      return argv;
    } catch (Throwable t) {
      throw new RuntimeException("Failed to get program arguments using JNA", t);
    }
  }

  private Kernel32 getKernel32() {
    if (kernel32 == null) {
      kernel32 = Native.loadLibrary("kernel32", Kernel32.class);
    }
    return kernel32;
  }

  private Shell32 getShell32() {
    if (shell32 == null) {
      shell32 = Native.loadLibrary("shell32", Shell32.class);
    }
    return shell32;
  }
}

interface Kernel32 extends StdCallLibrary {
  int GetCurrentProcessId();

  WString GetCommandLineW();

  Pointer LocalFree(Pointer pointer);
}

interface Shell32 extends StdCallLibrary {
  Pointer CommandLineToArgvW(WString command_line, IntByReference argc);
}
