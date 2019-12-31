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
package org.pgptool.gui.ui.decrypttext;

import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.Action;

import org.pgptool.gui.app.Message;
import org.pgptool.gui.ui.decryptonedialog.KeyAndPasswordCallback;

public interface DecryptTextHost {
	void handleClose();

	Action getActionToOpenCertificatesList();

	/**
	 * This method will allow to find out password for a key. If password is already
	 * cached it will be returned right away (through callback). Otherwise method
	 * will return null and call callback when user will provide password.
	 */
	void askUserForKeyAndPassword(Set<String> keysIds, Message purpose, KeyAndPasswordCallback keyAndPasswordCallback,
			ActionEvent originEvent);

	void openEncryptText(Set<String> recipientsList, ActionEvent originEvent);
}
