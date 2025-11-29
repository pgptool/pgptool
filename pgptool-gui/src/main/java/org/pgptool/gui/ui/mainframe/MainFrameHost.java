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
package org.pgptool.gui.ui.mainframe;

import java.awt.event.ActionEvent;
import java.util.Set;
import javax.swing.Action;

public interface MainFrameHost {
  void handleExitApp();

  Action getActionShowAboutInfo();

  Action getActionImportKey();

  Action getActionImportKeyFromText();

  Action getActionShowKeysList();

  Action getActionForEncrypt();

  Action getActionForEncryptText();

  Action getActionForDecrypt();

  Action getActionForDecryptText();

  Action getActionChangeFolderForDecrypted();

  Action getActionCheckForUpdates();

  void openEncryptDialogFor(String decryptedFile, ActionEvent originEvent);

  void openDecryptDialogFor(String encryptedFile, ActionEvent originEvent);

  Action getActionCreateKey();

  void openEncryptBackMultipleFor(Set<String> decryptedFiles, ActionEvent originEvent);

  Action getActionBuyMeCoffee();

  Action getActionFaq();

  Action getActionHelp();

  Action getActionReportIssue();

  Action getAskQuestionInChat();
}
