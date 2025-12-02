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
package org.pgptool.gui.ui.encrypttext;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.ui.tools.TextEditUxUtils;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.checklistbox.JCheckList;
import org.pgptool.gui.ui.tools.checklistbox.ModelMultSelInCheckListBinding;
import org.pgptool.gui.ui.tools.swingpm.DialogViewBaseEx;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class EncryptTextView extends DialogViewBaseEx<EncryptTextPm> {
  private JPanel pnl;

  private JCheckList<Key> recipients;
  private JScrollPane recipientsScroller;
  private JButton btnSelectRecipientsFromClipboard;

  private JTextArea edSourceText;
  private JButton btnPasteSourceFromClipboard;

  private JTextArea edTargetText;
  private JButton btnCopyTargetToClipboard;

  private JButton btnPerformOperation;
  private JButton btnCancel;

  @Override
  protected void internalInitComponents() {
    pnl = new JPanel(new BorderLayout());

    pnl.add(buildControllsPanel(), BorderLayout.CENTER);
    pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
  }

  private Component buildControllsPanel() {
    SgLayout sgl = new SgLayout(3, 3, spacing(1), 2);
    sgl.setColSize(0, spacing(30), SgLayout.SIZE_TYPE_CONSTANT);
    sgl.setColSize(1, spacing(50), SgLayout.SIZE_TYPE_WEIGHTED);
    sgl.setColSize(2, spacing(50), SgLayout.SIZE_TYPE_WEIGHTED);
    sgl.setRowSize(1, 100, SgLayout.SIZE_TYPE_WEIGHTED);

    JPanel ret = new JPanel(sgl);
    ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // recipients
    ret.add(new JLabel(text("term.recipients")), sgl.cs(0, 0));

    recipients = new JCheckList<>();
    recipientsScroller = new JScrollPane(recipients);
    ret.add(recipientsScroller, sgl.cs(0, 1));

    ret.add(btnSelectRecipientsFromClipboard = new JButton(), sgl.cs(0, 2));

    // source text
    ret.add(new JLabel(text("term.textToEncrypt")), sgl.cs(1, 0));
    ret.add(new JScrollPane(edSourceText = new JTextArea()), sgl.cs(1, 1));
    edSourceText.setFont(new JTextField().getFont());
    edSourceText.setMargin(new Insets(5, 5, 5, 5));
    edSourceText.setWrapStyleWord(true);
    edSourceText.setLineWrap(true);
    TextEditUxUtils.setCommonTextEditorActions(edSourceText);

    JPanel btns = new JPanel(new GridLayout(1, 1, 10, 0));
    btns.add(btnPasteSourceFromClipboard = new JButton());
    btns.add(btnPerformOperation = new JButton());
    ret.add(btns, sgl.cs(1, 2));

    // target text
    ret.add(new JLabel(text("term.encryptedText")), sgl.cs(2, 0));
    ret.add(new JScrollPane(edTargetText = new JTextArea()), sgl.cs(2, 1));
    edTargetText.setFont(new JTextField().getFont());
    edTargetText.setMargin(new Insets(5, 5, 5, 5));
    TextEditUxUtils.setCommonTextEditorActions(edTargetText);
    ret.add(btnCopyTargetToClipboard = new JButton(), sgl.cs(2, 2));

    // x. ret
    return ret;
  }

  private JPanel buildPanelButtons() {
    JPanel ret = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    ret.add(btnCancel = new JButton());
    return ret;
  }

  @Override
  protected void internalBindToPm() {
    super.internalBindToPm();

    bindingContext.add(
        new ModelMultSelInCheckListBinding<>(
            bindingContext, pm.getSelectedRecipients(), recipients));

    bindingContext.setupBinding(pm.getSourceText(), edSourceText);
    bindingContext.setupBinding(pm.getTargetText(), edTargetText);

    bindingContext.setupBinding(
        pm.actionSelectRecipientsFromClipboard, btnSelectRecipientsFromClipboard);
    bindingContext.setupBinding(pm.actionPasteSourceFromClipboard, btnPasteSourceFromClipboard);
    bindingContext.setupBinding(pm.actionCopyTargetToClipboard, btnCopyTargetToClipboard);

    bindingContext.setupBinding(pm.actionDoOperation, btnPerformOperation);
    bindingContext.setupBinding(pm.actionClose, btnCancel);
  }

  @Override
  protected JDialog initDialog(Window owner, Object constraints) {
    JDialog ret = new JDialog(owner, ModalityType.MODELESS);
    ret.setLayout(new BorderLayout());
    ret.setResizable(true);
    ret.setMinimumSize(
        new Dimension(UiUtils.getFontRelativeSize(80), UiUtils.getFontRelativeSize(40)));
    ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    ret.setTitle(Messages.get("action.encryptText"));
    ret.add(pnl, BorderLayout.CENTER);
    initWindowGeometryPersister(ret, "encrText");
    return ret;
  }

  @Override
  protected JPanel getRootPanel() {
    return pnl;
  }

  @Override
  protected void handleDialogShown() {
    super.handleDialogShown();
    edSourceText.requestFocusInWindow();
  }

  @Override
  protected void dispatchWindowCloseEvent(ActionEvent originAction) {
    btnCancel.getAction().actionPerformed(originAction);
  }
}
