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
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
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

  public OsNativeApiWindowsImpl() {}

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

  @Override
  public Charset findDefaultCharset() {
    int cp = getKernel32().GetACP();
    return charsetForWindowsCodePage(cp);
  }

  private static Charset charsetForWindowsCodePage(int cp) {
    try {
      // Special/common cases first
      if (cp == 65001) {
        return StandardCharsets.UTF_8; // UTF-8 ACP
      }
      if (cp == 1251) {
        return Charset.forName("windows-1251");
      }
      if (cp == 1252) {
        return Charset.forName("windows-1252");
      }
      if (cp == 1250) {
        return Charset.forName("windows-1250");
      }
      if (cp == 1254) {
        return Charset.forName("windows-1254");
      }
      if (cp == 1257) {
        return Charset.forName("windows-1257");
      }
      if (cp == 932) {
        // Japanese. Java's canonical name is windows-31j
        return Charset.forName("windows-31j");
      }
      if (cp == 936) {
        return Charset.forName("GBK");
      }
      if (cp == 949) {
        return Charset.forName("ms949");
      }
      if (cp == 950) {
        return Charset.forName("Big5");
      }

      // Generic attempts
      Charset cs = forNameOrNull("windows-" + cp);
      if (cs != null) {
        return cs;
      }
      cs = forNameOrNull("cp" + cp);
      if (cs != null) {
        return cs;
      }
      cs = forNameOrNull("ms" + cp);
      return cs;
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Charset forNameOrNull(String name) {
    try {
      return Charset.forName(name);
    } catch (IllegalCharsetNameException | UnsupportedCharsetException ex) {
      return null;
    }
  }
}

interface Kernel32 extends StdCallLibrary {
  int GetCurrentProcessId();

  WString GetCommandLineW();

  Pointer LocalFree(Pointer pointer);

  int GetACP();
}

interface Shell32 extends StdCallLibrary {
  Pointer CommandLineToArgvW(WString command_line, IntByReference argc);
}
