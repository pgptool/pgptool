/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
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
 *******************************************************************************/
package org.pgptool.gui.tools.osnative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

/**
 * 
 * @author Sergey Karpushin
 *
 */
public class OsNativeApiWindowsImpl implements OsNativeApi {
	private static Logger log = Logger.getLogger(OsNativeApiWindowsImpl.class);

	private Kernel32 kernel32;
	private Shell32 shell32;

	/**
	 * This method will try to solve issue when java executable cannot transfer
	 * argument in utf encoding. cyrillic languages screws up and application
	 * receives ??????? instead of real text
	 */
	@Override
	public String[] getCommandLineArguments(String[] fallBackTo) {
		try {
			log.debug("In case we fail fallback would happen to: " + Arrays.toString(fallBackTo));
			String[] ret = getFullCommandLine();
			log.debug("According to Windows API programm was started with arguments: " + Arrays.toString(ret));

			List<String> argsOnly = null;
			for (int i = 0; i < ret.length; i++) {
				if (argsOnly != null) {
					argsOnly.add(ret[i]);
				} else if (ret[i].toLowerCase().endsWith(".jar")) {
					argsOnly = new ArrayList<>();
				}
			}
			if (argsOnly != null) {
				ret = argsOnly.toArray(new String[0]);
			}

			log.debug("These arguments will be used: " + Arrays.toString(ret));
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
			kernel32 = (Kernel32) Native.loadLibrary("kernel32", Kernel32.class);
		}
		return kernel32;
	}

	private Shell32 getShell32() {
		if (shell32 == null) {
			shell32 = (Shell32) Native.loadLibrary("shell32", Shell32.class);
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
