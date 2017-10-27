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
package org.pgptool.gui.ui.decrypttext;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class DecryptTextView extends DialogViewBaseCustom<DecryptTextPm> {
	private JPanel pnl;

	private JTextArea edSourceText;
	private JTextArea edRecipients;

	private JTextArea edTargetText;
	private JButton btnCopyTargetToClipboard;

	private JButton btnPerformOperation;
	private JButton btnCancel;
	private JButton btnReply;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());

		pnl.add(buildControllsPanel(), BorderLayout.CENTER);
		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private Component buildControllsPanel() {
		SgLayout sgl = new SgLayout(2, 5, spacing(1), 2);
		sgl.setColSize(0, spacing(50), SgLayout.SIZE_TYPE_WEIGHTED);
		sgl.setColSize(1, spacing(50), SgLayout.SIZE_TYPE_WEIGHTED);
		sgl.setRowSize(1, 70, SgLayout.SIZE_TYPE_WEIGHTED);
		sgl.setRowSize(3, 30, SgLayout.SIZE_TYPE_WEIGHTED);

		JPanel ret = new JPanel(sgl);
		ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// source text
		ret.add(new JLabel(text("term.textToDecrypt")), sgl.cs(0, 0));
		ret.add(new JScrollPane(edSourceText = new JTextArea()), sgl.cs(0, 1));
		edSourceText.setMargin(new Insets(5, 5, 5, 5));

		// recipients
		ret.add(new JLabel(text("term.encryptedFor")), sgl.cs(0, 2));
		ret.add(new JScrollPane(edRecipients = new JTextArea()), sgl.cs(0, 3));
		edRecipients.setEditable(false);
		edRecipients.setBackground(UIManager.getColor ( "Panel.background" ));
		edRecipients.setMargin(new Insets(5, 5, 5, 5));
		edRecipients.setLineWrap(true);

		// decrypt button
		JPanel btns = new JPanel(new BorderLayout(10, 0));
		btns.add(btnPerformOperation = new JButton(), BorderLayout.CENTER);
		ret.add(btns, sgl.cs(0, 4));

		// target text
		ret.add(new JLabel(text("term.decryptedText")), sgl.cs(1, 0));
		ret.add(new JScrollPane(edTargetText = new JTextArea()), sgl.cs(1, 1, 1, 3));
		edTargetText.setMargin(new Insets(5, 5, 5, 5));
		
		btns = new JPanel(new GridLayout(1, 0, 10, 0));
		btns.add(btnCopyTargetToClipboard = new JButton());
		btns.add(btnReply = new JButton());
		ret.add(btns, sgl.cs(1, 4));
		
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

		bindingContext.setupBinding(pm.getSourceText(), edSourceText);
		bindingContext.setupBinding(pm.getTargetText(), edTargetText);
		bindingContext.setupBinding(pm.getRecipients(), edRecipients);

		bindingContext.setupBinding(pm.actionCopyTargetToClipboard, btnCopyTargetToClipboard);

		bindingContext.setupBinding(pm.actionDoOperation, btnPerformOperation);
		bindingContext.setupBinding(pm.actionCancel, btnCancel);
		bindingContext.setupBinding(pm.actionReply, btnReply);
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.MODELESS);
		ret.setLayout(new BorderLayout());
		ret.setResizable(true);
		ret.setSize(new Dimension(UiUtils.getFontRelativeSize(90), UiUtils.getFontRelativeSize(50)));
		ret.setMinimumSize(new Dimension(UiUtils.getFontRelativeSize(80), UiUtils.getFontRelativeSize(40)));
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.decryptText"));
		ret.add(pnl, BorderLayout.CENTER);
		ret.pack();
		UiUtils.centerWindow(ret);
		return ret;
	}

	@Override
	protected JPanel getRootPanel() {
		return pnl;
	}

	@Override
	protected void dispatchWindowCloseEvent() {
		btnCancel.getAction().actionPerformed(null);
	}

}
