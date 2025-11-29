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
package org.pgptool.gui.ui.getkeypassworddialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.dto.MatchedKey;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordManyKeysView;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordOneKeyView;
import org.pgptool.gui.ui.getkeypassword.GetKeyPasswordPm;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.DialogViewBaseEx;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import ru.skarpushin.swingpm.base.ViewBase;

public class GetKeyPasswordDialogView extends DialogViewBaseEx<GetKeyPasswordDialogPm>
    implements ApplicationContextAware {
  private ViewBase<GetKeyPasswordPm> passwordView;

  private JPanel pnl;

  private JButton buttonCancel;
  private JButton buttonOk;
  private Component requestFocusFor;

  private ApplicationContext applicationContext;

  @Override
  protected void internalInitComponents() {
    pnl = new JPanel(new BorderLayout());
  }

  @Override
  protected void internalBindToPm() {
    super.internalBindToPm();

    getPasswordView().renderTo(pnl, BorderLayout.CENTER);
    if (getPasswordView() instanceof GetKeyPasswordOneKeyView) {
      GetKeyPasswordOneKeyView subView = (GetKeyPasswordOneKeyView) getPasswordView();
      buttonCancel = subView.btnCancel;
      buttonOk = subView.btnPerformOperation;
      requestFocusFor = subView.edPassword;
    } else if (getPasswordView() instanceof GetKeyPasswordManyKeysView) {
      GetKeyPasswordManyKeysView subView = (GetKeyPasswordManyKeysView) getPasswordView();
      buttonCancel = subView.btnCancel;
      buttonOk = subView.btnPerformOperation;
      requestFocusFor = subView.edPassword;
    }

    SwingUtilities.invokeLater(
        () -> {
          if (dialog == null) {
            return;
          }
          dialog.pack();
          dialog.getRootPane().setDefaultButton(buttonOk);
          requestFocusFor.requestFocusInWindow();
        });
  }

  @Override
  public void unrender() {
    if (passwordView != null) {
      passwordView.unrender();
      passwordView = null;
    }
    super.unrender();
  }

  @Override
  protected JDialog initDialog(Window owner, Object constraints) {
    JDialog ret = new JDialog(owner, ModalityType.DOCUMENT_MODAL);
    ret.setLayout(new BorderLayout());
    ret.setResizable(false);
    ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    ret.setTitle(Messages.get("action.providePasswordForAKey"));
    ret.add(pnl, BorderLayout.CENTER);
    ret.pack();
    UiUtils.centerWindow(ret, owner);
    return ret;
  }

  @Override
  protected void handleDialogShown() {
    super.handleDialogShown();
    if (requestFocusFor != null) {
      requestFocusFor.requestFocusInWindow();
    }
  }

  @Override
  protected JPanel getRootPanel() {
    return pnl;
  }

  @Override
  protected void dispatchWindowCloseEvent(ActionEvent originAction) {
    buttonCancel.getAction().actionPerformed(originAction);
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.applicationContext = applicationContext;
  }

  public ViewBase<GetKeyPasswordPm> getPasswordView() {
    if (passwordView == null) {
      List<MatchedKey> matchedKeys = pm.getGetKeyPasswordPm().getMatchedKeys();
      if (matchedKeys != null && matchedKeys.size() == 1) {
        passwordView = applicationContext.getBean(GetKeyPasswordOneKeyView.class);
      } else {
        passwordView = applicationContext.getBean(GetKeyPasswordManyKeysView.class);
      }
      passwordView.setPm(pm.getGetKeyPasswordPm());
    }
    return passwordView;
  }
}
