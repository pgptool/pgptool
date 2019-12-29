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
package org.pgptool.gui.app;

import org.summerb.i18n.HasMessageArgs;
import org.summerb.i18n.HasMessageCode;

public class Message implements HasMessageCode, HasMessageArgs {
	private String code;
	private Object[] args;

	public Message(String code, Object[] args) {
		this.code = code;
		this.args = args;
	}

	public Message(String code) {
		this.code = code;
	}

	@Override
	public Object[] getMessageArgs() {
		return args;
	}

	@Override
	public String getMessageCode() {
		return code;
	}

}
