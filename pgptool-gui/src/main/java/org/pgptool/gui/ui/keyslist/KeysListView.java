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
package org.pgptool.gui.ui.keyslist;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.swingpm.DialogViewBaseEx;
import org.springframework.beans.factory.annotation.Autowired;

public class KeysListView extends DialogViewBaseEx<KeysListPm> {
	private JPanel panelRoot;

	private JMenuBar menuBar;
	private JMenuItem miImport;
	private JMenuItem miImportFromText;
	private JMenuItem miCreate;
	private JMenuItem miExportPublicKeys;
	private JMenuItem miClose;

	@Autowired
	private KeysTableView keysTableView;

	@Override
	protected void internalInitComponents() {
		panelRoot = new JPanel(new BorderLayout());
		initMenuBar();
		keysTableView.setPersistenceCode("keyRing");
		keysTableView.renderTo(panelRoot, BorderLayout.CENTER);
	}

	private void initMenuBar() {
		menuBar = new JMenuBar();
		JMenu menuTs = new JMenu(Messages.get("term.actions"));
		menuTs.add(miImport = new JMenuItem());
		menuTs.add(miImportFromText = new JMenuItem());
		menuTs.add(miCreate = new JMenuItem());
		menuTs.addSeparator();
		menuTs.add(miExportPublicKeys = new JMenuItem());
		menuTs.addSeparator();
		menuTs.add(miClose = new JMenuItem());
		menuBar.add(menuTs);
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();
		keysTableView.setPm(pm.getKeysTablePm());
		bindingContext.setupBinding(pm.getActionImport(), miImport);
		bindingContext.setupBinding(pm.getActionImportFromText(), miImportFromText);
		bindingContext.setupBinding(pm.getActionCreate(), miCreate);
		bindingContext.setupBinding(pm.actionExportAllPublicKeys, miExportPublicKeys);

		bindingContext.setupBinding(pm.getActionClose(), miClose);
	}

	@Override
	protected void internalUnbindFromPm() {
		super.internalUnbindFromPm();
		keysTableView.detach();
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setMinimumSize(new Dimension(spacing(50), spacing(25)));
		ret.setLayout(new BorderLayout());
		ret.setResizable(true);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("term.keysList"));
		ret.add(panelRoot, BorderLayout.CENTER);
		ret.setJMenuBar(menuBar);
		initWindowGeometryPersister(ret, "keysList");
		return ret;
	}

	@Override
	protected JPanel getRootPanel() {
		return panelRoot;
	}

	@Override
	protected void dispatchWindowCloseEvent(ActionEvent originAction) {
		miClose.getAction().actionPerformed(originAction);
	}

}
