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
package org.pgptool.gui.ui.tools.browsefs;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.JFileChooser;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.UiUtils;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

public class FolderChooserDialog {
  private ValueAdapter<String> optionalRecentlyUsedFolder;
  private String title;

  public FolderChooserDialog(
      String optionalTitle, ValueAdapter<String> optionalRecentlyUsedFolder) {
    this.title = optionalTitle != null ? optionalTitle : Messages.get("term.selectFolder");
    this.optionalRecentlyUsedFolder = optionalRecentlyUsedFolder;
  }

  public String askUserForFolder(ActionEvent originEvent) {
    JFileChooser ofd = new JFileChooser();
    ofd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    ofd.setAcceptAllFileFilterUsed(true);
    ofd.setMultiSelectionEnabled(false);
    ofd.setDialogTitle(title);
    ofd.setApproveButtonText(Messages.get("action.choose"));
    onPostConstruct(ofd);

    int result = ofd.showOpenDialog(UiUtils.findWindow(originEvent));
    if (result != JFileChooser.APPROVE_OPTION) {
      return onFolderChosen(null);
    }

    File retFile = ofd.getSelectedFile();
    if (retFile == null) {
      return onFolderChosen(null);
    }

    String ret = retFile.getAbsolutePath();
    return onFolderChosen(ret);
  }

  protected void onPostConstruct(JFileChooser ofd) {
    if (optionalRecentlyUsedFolder != null) {
      String startFolder = optionalRecentlyUsedFolder.getValue();
      if (startFolder != null) {
        ofd.setCurrentDirectory(new File(startFolder));
        ofd.setSelectedFile(new File(startFolder));
      }
    }
  }

  protected String onFolderChosen(String folder) {
    if (folder != null && optionalRecentlyUsedFolder != null) {
      optionalRecentlyUsedFolder.setValue(folder);
    }
    return folder;
  }
}
