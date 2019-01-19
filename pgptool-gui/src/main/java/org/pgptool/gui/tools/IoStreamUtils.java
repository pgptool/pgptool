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

import java.io.Closeable;
import java.io.IOException;

import org.apache.log4j.Logger;

public class IoStreamUtils {
	private static Logger log = Logger.getLogger(IoStreamUtils.class);

	public static void safeClose(Closeable stream) {
		if (stream == null) {
			return;
		}
		try {
			stream.close();
		} catch (IOException t) {
			log.debug("Failed to close the stream", t);
		} catch (Throwable t) {
			throw new RuntimeException("Something bad hapened while trying to close the stream", t);
		}
	}
}
