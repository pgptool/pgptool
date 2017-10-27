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
package org.pgptool.gui.ui.encryptone;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.ui.tools.ControlsDisabler;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class EncryptOneView extends DialogViewBaseCustom<EncryptOnePm> {
	private JPanel pnl;

	private JPanel controlsPanel;
	private TypedPropertyChangeListener<Boolean> isDisableControlsChanged;
	
	private JTextField edSourceFile;
	private JButton btnBrowseSource;

	private JCheckBox chkUseSameFolder;
	private JTextField edTargetFile;
	private JButton btnBrowseTarget;

	private JList<Key<KeyData>> recipients;
	private JScrollPane recipientsScroller;

	private JCheckBox chkDeleteSourceAfter;
	private JCheckBox chkOpenTargetFolderAfter;

	private JButton btnPerformOperation;
	private JButton btnCancel;

	private JProgressBar pbar;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());

		pnl.add(controlsPanel = buildControllsPanel(), BorderLayout.CENTER);
		isDisableControlsChanged = new ControlsDisabler(controlsPanel);
		
		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private JPanel buildControllsPanel() {
		SgLayout sgl = new SgLayout(2, 10, spacing(1), 2);
		sgl.setColSize(0, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(1, spacing(30), SgLayout.SIZE_TYPE_WEIGHTED);

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
		ret.add(chkUseSameFolder = new JCheckBox(), sgl.cs(1, row));
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
		ret.add(new JLabel(text("term.recipients")), sgl.cs(0, row));
		recipients = new JList<>();
		recipients.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		recipientsScroller = new JScrollPane(recipients);
		ret.add(recipientsScroller, sgl.cs(1, row, 1, 2));
		row++;
		sgl.setRowSize(row, 30, SgLayout.SIZE_TYPE_WEIGHTED);

		// spacing
		row++;
		ret.add(buildEmptyLine(), sgl.cs(0, row, 2, 1));

		// after actions
		row++;
		ret.add(new JLabel(text("term.afterOperationPostAction")), sgl.cs(0, row));
		ret.add(chkDeleteSourceAfter = new JCheckBox(), sgl.cs(1, row));
		row++;
		ret.add(chkOpenTargetFolderAfter = new JCheckBox(), sgl.cs(1, row));
		// chkDeleteSourceAfter.setBorder(BorderFactory.createEmptyBorder());
		// chkOpenTargetFolderAfter.setBorder(BorderFactory.createEmptyBorder());

		// x. ret
		return ret;
	}

	private Component buildEmptyLine() {
		JPanel ret = new JPanel();
		ret.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.black));
		return ret;
	}

	private JPanel buildPanelButtons() {
		JPanel bottomPanel = new JPanel(new BorderLayout(0, 0));
		bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel panelButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		panelButtons.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
		panelButtons.add(btnPerformOperation = new JButton());
		panelButtons.add(btnCancel = new JButton());
		bottomPanel.add(panelButtons, BorderLayout.EAST);

		bottomPanel.add(pbar = new JProgressBar(0, 100), BorderLayout.CENTER);
		pbar.setStringPainted(true);

		return bottomPanel;
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getSourceFile(), edSourceFile);
		bindingContext.setupBinding(pm.actionBrowseSource, btnBrowseSource);

		bindingContext.setupBinding(pm.getIsUseSameFolder(), chkUseSameFolder);
		bindingContext.setupBinding(pm.getTargetFile(), edTargetFile);
		bindingContext.registerPropertyValuePropagation(pm.getTargetFileEnabled(), edTargetFile, "enabled");
		bindingContext.setupBinding(pm.actionBrowseTarget, btnBrowseTarget);

		bindingContext.setupBinding(pm.getSelectedRecipients(), recipients);

		bindingContext.setupBinding(pm.getIsDeleteSourceAfter(), chkDeleteSourceAfter);
		bindingContext.setupBinding(pm.getIsOpenTargetFolderAfter(), chkOpenTargetFolderAfter);

		bindingContext.setupBinding(pm.actionDoOperation, btnPerformOperation);
		bindingContext.setupBinding(pm.actionCancel, btnCancel);

		bindingContext.registerPropertyValuePropagation(pm.getIsProgressVisible(), pbar, "visible");
		bindingContext.registerPropertyValuePropagation(pm.getProgressNote(), pbar, "string");
		bindingContext.registerPropertyValuePropagation(pm.getProgressValue(), pbar, "value");
		pbar.setVisible(false);

		bindingContext.registerOnChangeHandler(pm.getIsDisableControls(), isDisableControlsChanged);
		isDisableControlsChanged.handlePropertyChanged(this, null, false, pm.getIsDisableControls().getValue());
	}
	
	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.MODELESS);
		ret.setLayout(new BorderLayout());
		ret.setResizable(true);
		ret.setSize(new Dimension(UiUtils.getFontRelativeSize(60), UiUtils.getFontRelativeSize(50)));
		ret.setMinimumSize(new Dimension(UiUtils.getFontRelativeSize(50), UiUtils.getFontRelativeSize(40)));
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("action.encrypt"));
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
