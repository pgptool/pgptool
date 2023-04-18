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
package org.pgptool.gui.ui.importkey;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.keyslist.KeysTableView;
import org.pgptool.gui.ui.tools.swingpm.DialogViewBaseEx;
import org.springframework.beans.factory.annotation.Autowired;

public class KeyImporterView extends DialogViewBaseEx<KeyImporterPm> {
	private JPanel pnl;

	@Autowired
	private KeysTableView keysTableView;

	private JButton btnImport;
	private JButton btnCancel;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());
		pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		keysTableView.setPersistenceCode("keyImprt");
		keysTableView.renderTo(pnl, BorderLayout.CENTER);

		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private JPanel buildPanelButtons() {
		JPanel ret = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		ret.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		ret.add(btnImport = new JButton());
		ret.add(btnCancel = new JButton());
		return ret;
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		keysTableView.setPm(pm.getKeysTablePm());

		bindingContext.setupBinding(pm.getActionDoImport(), btnImport);
		bindingContext.setupBinding(pm.getActionCancel(), btnCancel);
	}

	@Override
	protected void internalUnbindFromPm() {
		super.internalUnbindFromPm();
		keysTableView.detach();
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setLayout(new BorderLayout());
		ret.setResizable(true);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.importKey"));
		ret.add(pnl, BorderLayout.CENTER);
		ret.setMinimumSize(new Dimension(spacing(60), spacing(30)));

		initWindowGeometryPersister(ret, "keyImprt");

		return ret;
	}

	@Override
	protected JPanel getRootPanel() {
		return pnl;
	}

	@Override
	protected void dispatchWindowCloseEvent(ActionEvent originAction) {
		btnCancel.getAction().actionPerformed(originAction);
	}

}
