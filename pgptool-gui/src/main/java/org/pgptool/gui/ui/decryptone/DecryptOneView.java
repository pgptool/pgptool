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
package org.pgptool.gui.ui.decryptone;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class DecryptOneView extends DialogViewBaseCustom<DecryptOnePm> {
	private JPanel pnl;

	private JPanel ajaxPanel;

	private JPanel controlsPanel;
	private JTextField edSourceFile;
	private JButton btnBrowseSource;

	private ButtonGroup btnGroupTargetFolder = new ButtonGroup();
	private JRadioButton chkUseSameFolder;
	private JRadioButton chkUseTempFolder;
	private JRadioButton chkUseBrowseFolder;
	private JTextField edTargetFile;
	private JButton btnBrowseTarget;

	private JComboBox<Key<KeyData>> decryptionKey;
	private JPasswordField edPassword;

	private JCheckBox chkDeleteSourceAfter;
	private JCheckBox chkOpenTargetFolderAfter;
	private JCheckBox chkOpenAssociatedApplication;

	private JButton btnPerformOperation;
	private JButton btnCancel;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());

		pnl.add(buildControllsPanel(), BorderLayout.CENTER);
		buildAjaxIndicatorPanel();
		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private void buildAjaxIndicatorPanel() {
		ajaxPanel = new JPanel(new BorderLayout());
		ajaxPanel.add(new JLabel(text("phrase.pleaseWait")));
	}

	private Component buildControllsPanel() {
		SgLayout sgl = new SgLayout(2, 11, spacing(1), 2);
		sgl.setColSize(0, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(1, spacing(60), SgLayout.SIZE_TYPE_CONSTANT);

		JPanel ret = new JPanel(sgl);
		ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		int row = 0;

		// source file
		ret.add(new JLabel(text("term.sourceFile")), sgl.cs(0, row));
		JPanel pnlSourceFile = new JPanel(new BorderLayout());
		pnlSourceFile.add(edSourceFile = new JTextField(), BorderLayout.CENTER);
		pnlSourceFile.add(btnBrowseSource = new JButton(), BorderLayout.EAST);
		ret.add(pnlSourceFile, sgl.cs(1, row));

		// spacing
		row++;
		ret.add(buildEmptyLine(), sgl.cs(0, row, 2, 1));

		// target file
		row++;
		ret.add(new JLabel(text("term.targetFile")), sgl.cs(0, row));
		JPanel targetRadios = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		targetRadios.add(chkUseTempFolder = new JRadioButton());
		targetRadios.add(new JLabel("   "));
		targetRadios.add(chkUseSameFolder = new JRadioButton());
		targetRadios.add(new JLabel("   "));
		targetRadios.add(chkUseBrowseFolder = new JRadioButton());
		btnGroupTargetFolder.add(chkUseTempFolder);
		btnGroupTargetFolder.add(chkUseSameFolder);
		btnGroupTargetFolder.add(chkUseBrowseFolder);
		ret.add(targetRadios, sgl.cs(1, row));
		row++;
		JPanel pnlTargetFile = new JPanel(new BorderLayout());
		pnlTargetFile.add(edTargetFile = new JTextField(), BorderLayout.CENTER);
		pnlTargetFile.add(btnBrowseTarget = new JButton(), BorderLayout.EAST);
		ret.add(pnlTargetFile, sgl.cs(1, row));

		// spacing
		row++;
		ret.add(buildEmptyLine(), sgl.cs(0, row, 2, 1));

		// recipients
		row++;
		ret.add(new JLabel(text("term.decryptionKey")), sgl.cs(0, row));
		ret.add(decryptionKey = new JComboBox<>(), sgl.cs(1, row));
		row++;
		JPanel panelPassword = new JPanel(new BorderLayout(spacing(1), 0));
		panelPassword.add(new JLabel(text("term.password")), BorderLayout.WEST);
		panelPassword.add(edPassword = new JPasswordField(), BorderLayout.CENTER);
		ret.add(panelPassword, sgl.cs(1, row));

		// spacing
		row++;
		ret.add(buildEmptyLine(), sgl.cs(0, row, 2, 1));

		// after actions
		row++;
		ret.add(new JLabel(text("term.afterOperationPostAction")), sgl.cs(0, row));
		ret.add(chkDeleteSourceAfter = new JCheckBox(), sgl.cs(1, row));
		row++;
		ret.add(chkOpenTargetFolderAfter = new JCheckBox(), sgl.cs(1, row));
		row++;
		ret.add(chkOpenAssociatedApplication = new JCheckBox(), sgl.cs(1, row));

		// x. ret
		return controlsPanel = ret;
	}

	private Component buildEmptyLine() {
		JPanel ret = new JPanel();
		ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
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

		bindingContext.setupBinding(pm.getSourceFile(), edSourceFile);
		bindingContext.setupBinding(pm.actionBrowseSource, btnBrowseSource);

		bindingContext.setupBinding(pm.getIsUseSameFolder(), chkUseSameFolder);
		bindingContext.setupBinding(pm.getIsUseTempFolder(), chkUseTempFolder);
		bindingContext.setupBinding(pm.getIsUseBrowseFolder(), chkUseBrowseFolder);

		bindingContext.setupBinding(pm.getTargetFile(), edTargetFile);
		bindingContext.registerPropertyValuePropagation(pm.getTargetFileEnabled(), edTargetFile, "enabled");
		bindingContext.setupBinding(pm.actionBrowseTarget, btnBrowseTarget);

		bindingContext.setupBinding(pm.getSelectedKey(), decryptionKey);
		bindingContext.setupBinding(pm.getPassword(), edPassword);

		bindingContext.setupBinding(pm.getIsDeleteSourceAfter(), chkDeleteSourceAfter);
		bindingContext.setupBinding(pm.getIsOpenTargetFolderAfter(), chkOpenTargetFolderAfter);
		bindingContext.setupBinding(pm.getIsOpenAssociatedApplication(), chkOpenAssociatedApplication);

		bindingContext.setupBinding(pm.actionDoOperation, btnPerformOperation);
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
		JDialog ret = new JDialog(owner, ModalityType.MODELESS);
		ret.setLayout(new BorderLayout());
		ret.setResizable(false);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.decrypt"));
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
