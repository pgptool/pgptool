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
package org.pgptool.gui.ui.getkeypassword;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.jdesktop.swingx.JXLabel;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class GetKeyPasswordManyKeysView extends DialogViewBaseCustom<GetKeyPasswordPm> {
	private JPanel pnl;

	private JXLabel purpose;
	private JComboBox<Key<KeyData>> decryptionKey;
	private JPasswordField edPassword;

	private JButton btnPerformOperation;
	private JButton btnCancel;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());

		pnl.add(buildControllsPanel(), BorderLayout.CENTER);
		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private Component buildControllsPanel() {
		SgLayout sgl = new SgLayout(2, 3, spacing(1), 2);
		sgl.setColSize(0, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(1, spacing(40), SgLayout.SIZE_TYPE_CONSTANT);

		JPanel ret = new JPanel(sgl);
		ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		int row = 0;

		ret.add(purpose = new JXLabel(), sgl.cs(0, row, 2, 1));
		purpose.setLineWrap(true);
		row++;
		ret.add(new JLabel(text("term.key")), sgl.cs(0, row));
		ret.add(decryptionKey = new JComboBox<>(), sgl.cs(1, row));
		row++;
		ret.add(new JLabel(text("term.password")), sgl.cs(0, row));
		ret.add(edPassword = new JPasswordField(), sgl.cs(1, row));

		// x. ret
		return ret;
	}

	private JPanel buildPanelButtons() {
		JPanel ret = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		ret.add(btnPerformOperation = new JButton());
		ret.add(btnCancel = new JButton());
		return ret;
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getSelectedKey(), decryptionKey);
		bindingContext.setupBinding(pm.getPassword(), edPassword);
		bindingContext.setupBinding(pm.getPurpose(), purpose);

		bindingContext.setupBinding(pm.actionChooseKey, btnPerformOperation);
		bindingContext.setupBinding(pm.actionCancel, btnCancel);
	}

	@Override
	protected void handleDialogShown() {
		super.handleDialogShown();
		dialog.getRootPane().setDefaultButton(btnPerformOperation);
		edPassword.requestFocusInWindow();
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setLayout(new BorderLayout());
		ret.setResizable(false);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.chooseKeyAndPassword"));
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
