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

import java.io.FileNotFoundException;
import java.io.OutputStream;

/**
 * This interface will return stream for given file. It can only create 1 stream for 1 file,
 * subsequent calls will fail
 *
 * @author sergeyk
 */
public interface OutputStreamSupervisor {

  OutputStream get(String fileName) throws FileNotFoundException;

  OutputStream get(OutputStream target);
}
