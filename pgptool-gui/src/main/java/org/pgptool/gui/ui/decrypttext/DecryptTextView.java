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

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.jdesktop.swingx.JXLabel;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.encryptone.EncryptOneView;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.TextEditUxUtils;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class DecryptTextView extends DialogViewBaseCustom<DecryptTextPm> {
	private JPanel pnl;

	private JTextArea edSourceText;
	private JTextArea edRecipients;

	private JTextArea edTargetText;
	private JButton btnCopyTargetToClipboard;

	private JButton btnPasteAndDecrypt;
	private JButton btnDecrypt;
	private JButton btnCancel;
	private JButton btnReply;

	private JPanel panelMissingPrivateKeyWarning;
	private JPanel recipientsBlock;

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
		edSourceText.setFont(new JTextField().getFont());
		edSourceText.setMargin(new Insets(5, 5, 5, 5));
		TextEditUxUtils.setCommonTextEditorActions(edSourceText);

		// recipients
		ret.add(new JLabel(text("term.encryptedFor")), sgl.cs(0, 2));
		recipientsBlock = new JPanel(new BorderLayout());
		recipientsBlock.add(new JScrollPane(edRecipients = new JTextArea()), BorderLayout.CENTER);
		edRecipients.setFont(new JTextField().getFont());
		edRecipients.setEditable(false);
		edRecipients.setBackground(UIManager.getColor("Panel.background"));
		edRecipients.setMargin(new Insets(5, 5, 5, 5));
		edRecipients.setLineWrap(true);
		ret.add(recipientsBlock, sgl.cs(0, 3));

		JXLabel lblNoPrivateKeysSelectedWarning = new JXLabel(text("phrase.noMatchingPrivateKey"));
		lblNoPrivateKeysSelectedWarning.setLineWrap(true);
		panelMissingPrivateKeyWarning = EncryptOneView.wrapIntoMessagePanel(lblNoPrivateKeysSelectedWarning,
				new Color(253, 221, 191));

		// decrypt button
		JPanel btns = new JPanel(new BorderLayout(10, 0));
		btns.add(btnPasteAndDecrypt = new JButton(), BorderLayout.CENTER);
		btns.add(btnDecrypt = new JButton(), BorderLayout.EAST);
		ret.add(btns, sgl.cs(0, 4));

		// target text
		ret.add(new JLabel(text("term.decryptedText")), sgl.cs(1, 0));
		ret.add(new JScrollPane(edTargetText = new JTextArea()), sgl.cs(1, 1, 1, 3));
		edTargetText.setFont(new JTextField().getFont());
		edTargetText.setMargin(new Insets(5, 5, 5, 5));
		edTargetText.setWrapStyleWord(true);
		edTargetText.setLineWrap(true);
		TextEditUxUtils.setCommonTextEditorActions(edTargetText);

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

		bindingContext.setupBinding(pm.actionPasteAndDecrypt, btnPasteAndDecrypt);
		bindingContext.setupBinding(pm.actionDecrypt, btnDecrypt);

		bindingContext.setupBinding(pm.actionCancel, btnCancel);
		bindingContext.setupBinding(pm.actionReply, btnReply);

		bindingContext.registerOnChangeHandler(pm.getIsShowMissingPrivateKeyWarning(), onShowMissingPrivateKeyWarning);
	}

	@Override
	protected void handleDialogShown() {
		super.handleDialogShown();
		// I had to put it here because otherwise there is a weird resizing glitch
		// appears in rare case
		onShowMissingPrivateKeyWarning.handlePropertyChanged(pm,
				pm.getIsShowMissingPrivateKeyWarning().getPropertyName(), null,
				pm.getIsShowMissingPrivateKeyWarning().getValue());
	}

	private TypedPropertyChangeListener<Boolean> onShowMissingPrivateKeyWarning = new TypedPropertyChangeListener<Boolean>() {
		@Override
		public void handlePropertyChanged(Object source, String propertyName, Boolean oldValue, Boolean newValue) {
			if (newValue) {
				recipientsBlock.add(panelMissingPrivateKeyWarning, BorderLayout.SOUTH);
			} else {
				recipientsBlock.remove(panelMissingPrivateKeyWarning);
			}
			recipientsBlock.validate();
		}
	};

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.MODELESS);
		ret.setLayout(new BorderLayout());
		ret.setResizable(true);
		ret.setMinimumSize(new Dimension(UiUtils.getFontRelativeSize(80), UiUtils.getFontRelativeSize(40)));
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.decryptText"));
		ret.add(pnl, BorderLayout.CENTER);
		initWindowGeometryPersister(ret, "decrText");
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
