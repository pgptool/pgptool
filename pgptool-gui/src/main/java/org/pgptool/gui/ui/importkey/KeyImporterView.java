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
package org.pgptool.gui.ui.importkey;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class KeyImporterView extends DialogViewBaseCustom<KeyImporterPm> {
	private JPanel pnl;

	private JLabel edFileToImport;
	private JButton btnBrowse;

	private JPanel panelKeyInfo;
	private JLabel user;
	private JLabel keyId;

	private JLabel keyType;
	private JLabel keyAlgorithm;

	private JLabel createdOn;
	private JLabel expiresAt;

	private JButton btnImport;
	private JButton btnCancel;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());

		pnl.add(buildPanelFile(), BorderLayout.NORTH);
		pnl.add(buildPanelKeyInfo(), BorderLayout.CENTER);
		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private JPanel buildPanelFile() {
		SgLayout sgl = new SgLayout(3, 1, spacing(1), 2);
		sgl.setColSize(0, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(1, spacing(50), SgLayout.SIZE_TYPE_WEIGHTED);
		sgl.setColSize(2, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);

		JPanel ret = new JPanel(sgl);
		ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		ret.add(lbl("term.filePathName"), sgl.cs(0, 0));
		ret.add(edFileToImport = new JLabel(), sgl.cs(1, 0));
		ret.add(btnBrowse = new JButton(), sgl.cs(2, 0));
		return ret;
	}

	private JPanel buildPanelKeyInfo() {
		SgLayout sgl = new SgLayout(4, 3, spacing(1), 2);
		sgl.setColSize(0, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(1, spacing(20), SgLayout.SIZE_TYPE_CONSTANT);
		sgl.setColSize(2, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(3, spacing(15), SgLayout.SIZE_TYPE_CONSTANT);

		JPanel ret = new JPanel(sgl);
		ret.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

		ret.add(lbl("term.user"), sgl.cs(0, 0));
		ret.add(user = new JLabel(), sgl.cs(1, 0));
		ret.add(lbl("term.keyId"), sgl.cs(2, 0));
		ret.add(keyId = new JLabel(), sgl.cs(3, 0));

		ret.add(lbl("term.keyType"), sgl.cs(0, 1));
		ret.add(keyType = new JLabel(), sgl.cs(1, 1));
		ret.add(lbl("term.keyAlgorithm"), sgl.cs(2, 1));
		ret.add(keyAlgorithm = new JLabel(), sgl.cs(3, 1));

		ret.add(lbl("term.createdOn"), sgl.cs(0, 2));
		ret.add(createdOn = new JLabel(), sgl.cs(1, 2));
		ret.add(lbl("term.expiresAt"), sgl.cs(2, 2));
		ret.add(expiresAt = new JLabel(), sgl.cs(3, 2));

		return panelKeyInfo = ret;
	}

	private JLabel lbl(String code) {
		return new JLabel(Messages.get(code), SwingConstants.RIGHT);
	}

	private JPanel buildPanelButtons() {
		JPanel ret = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		ret.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
		ret.add(btnImport = new JButton());
		ret.add(btnCancel = new JButton());
		return ret;
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getFilePathName(), edFileToImport);
		bindingContext.setupBinding(pm.getActionBrowse(), btnBrowse);

		bindingContext.registerPropertyValuePropagation(pm.getIsKeyLoaded(), panelKeyInfo, "visible");
		bindingContext.setupBinding(pm.getUser(), user);
		bindingContext.setupBinding(pm.getKeyId(), keyId);
		bindingContext.setupBinding(pm.getKeyType(), keyType);
		bindingContext.setupBinding(pm.getKeyAlgorithm(), keyAlgorithm);
		bindingContext.setupBinding(pm.getCreatedOn(), createdOn);
		bindingContext.setupBinding(pm.getExpiresAt(), expiresAt);

		bindingContext.setupBinding(pm.getActionDoImport(), btnImport);
		bindingContext.setupBinding(pm.getActionCancel(), btnCancel);
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setLayout(new BorderLayout());
		ret.setResizable(false);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.importKey"));
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
