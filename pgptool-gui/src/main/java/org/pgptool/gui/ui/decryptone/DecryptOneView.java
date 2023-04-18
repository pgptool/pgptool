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
package org.pgptool.gui.ui.decryptone;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.pgptool.gui.ui.tools.ControlsDisabler;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.ViewBaseEx;

import ru.skarpushin.swingpm.base.HasWindow;
import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class DecryptOneView extends ViewBaseEx<DecryptOnePm> implements HasWindow {
	private JPanel pnl;

	private JPanel controlsPanel;
	private TypedPropertyChangeListener<Boolean> isDisableControlsChanged;
	private JProgressBar pbar;

	private JTextField edSourceFile;
	private JButton btnBrowseSource;

	private ButtonGroup btnGroupTargetFolder = new ButtonGroup();
	private JRadioButton chkUseSameFolder;
	private JRadioButton chkUseTempFolder;
	private JRadioButton chkUseBrowseFolder;
	private JTextField edTargetFile;
	private JButton btnBrowseTarget;

	private JCheckBox chkDeleteSourceAfter;
	private JCheckBox chkOpenTargetFolderAfter;
	private JCheckBox chkOpenAssociatedApplication;

	public JButton btnPerformOperation;
	public JButton btnCancel;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());

		pnl.add(controlsPanel = buildControllsPanel(), BorderLayout.CENTER);
		isDisableControlsChanged = new ControlsDisabler(controlsPanel);

		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private JPanel buildControllsPanel() {
		SgLayout sgl = new SgLayout(2, 8, spacing(1), 2);
		sgl.setColSize(0, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(1, spacing(40), SgLayout.SIZE_TYPE_CONSTANT);

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

		// after actions
		row++;
		ret.add(new JLabel(text("term.afterOperationPostAction")), sgl.cs(0, row));
		ret.add(chkDeleteSourceAfter = new JCheckBox(), sgl.cs(1, row));
		row++;
		ret.add(chkOpenTargetFolderAfter = new JCheckBox(), sgl.cs(1, row));
		row++;
		ret.add(chkOpenAssociatedApplication = new JCheckBox(), sgl.cs(1, row));

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
		bindingContext.setupBinding(pm.getIsUseTempFolder(), chkUseTempFolder);
		bindingContext.setupBinding(pm.getIsUseBrowseFolder(), chkUseBrowseFolder);

		bindingContext.setupBinding(pm.getTargetFile(), edTargetFile);
		bindingContext.registerPropertyValuePropagation(pm.getTargetFileEnabled(), edTargetFile, "enabled");
		bindingContext.setupBinding(pm.actionBrowseTarget, btnBrowseTarget);

		bindingContext.setupBinding(pm.getIsDeleteSourceAfter(), chkDeleteSourceAfter);
		bindingContext.setupBinding(pm.getIsOpenTargetFolderAfter(), chkOpenTargetFolderAfter);
		bindingContext.setupBinding(pm.getIsOpenAssociatedApplication(), chkOpenAssociatedApplication);

		bindingContext.setupBinding(pm.actionDoOperation, btnPerformOperation);
		bindingContext.setupBinding(pm.actionCancel, btnCancel);

		bindingContext.registerPropertyValuePropagation(pm.getIsProgressVisible(), pbar, "visible");
		bindingContext.registerPropertyValuePropagation(pm.getProgressNote(), pbar, "string");
		bindingContext.registerPropertyValuePropagation(pm.getProgressValue(), pbar, "value");
		pbar.setVisible(false);

		bindingContext.registerOnChangeHandler(pm.getIsDisableControls(), isDisableControlsChanged);
		isDisableControlsChanged.handlePropertyChanged(this, null, false, pm.getIsDisableControls().getValue());
	}

	public static int spacing(int lettersCount) {
		return UiUtils.getFontRelativeSize(lettersCount);
	}

	// TBD: dialog.getRootPane().setDefaultButton(btnPerformOperation);

	@Override
	protected void internalRenderTo(Container owner, Object constraints) {
		owner.add(pnl, constraints);
	}

	/**
	 * NOTE: Although we don't really have window, we still impl this so that
	 * dialog, initiated from this view will be shown in a proper position
	 */
	@Override
	public Window getWindow() {
		return UiUtils.findWindow(pnl.getParent());
	}

	@Override
	protected void internalUnrender() {
		pnl.getParent().remove(pnl);
	}

}
