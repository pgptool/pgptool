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

import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.pgptool.gui.ui.tools.UiUtils;

public class SplashScreenView extends JFrame {
  private static final long serialVersionUID = -1059755470997596254L;

  public SplashScreenView() {
    SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {
            initComponents();
          }
        });
  }

  private void initComponents() {
    setSize(300, 50);
    setLayout(new BorderLayout());
    UiUtils.centerWindow(this, null);
    setResizable(false);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    setUndecorated(true);
    JLabel label;
    add(label = new JLabel("Loading..."));
    label.setHorizontalAlignment(JLabel.CENTER);
    setVisible(true);
  }

  public void close() {
    this.setVisible(false);
    this.dispose();
  }
}
