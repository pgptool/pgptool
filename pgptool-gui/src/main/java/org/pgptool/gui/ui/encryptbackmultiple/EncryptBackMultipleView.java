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
package org.pgptool.gui.ui.encryptbackmultiple;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.jdesktop.swingx.JXLabel;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.ControlsDisabler;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class EncryptBackMultipleView extends DialogViewBaseCustom<EncryptBackMultiplePm> {
	private JPanel pnl;

	private JXLabel lblSourceFilesSummaru;
	private JXLabel lblRecipientsSummary;
	private JCheckBox chkIgnoreMissingRecipients;
	private JCheckBox chkDeleteSourceAfter;

	private JButton btnPerformOperation;
	private JButton btnCancel;

	private JProgressBar pbar;

	private JPanel panelControls;
	private TypedPropertyChangeListener<Boolean> isDisableControlsChanged;

	@Override
	protected void internalInitComponents() {
		pnl = new JPanel(new BorderLayout());

		panelControls = buildControllsPanel();
		isDisableControlsChanged = new ControlsDisabler(panelControls);
		pnl.add(panelControls, BorderLayout.CENTER);
		pnl.add(buildPanelButtons(), BorderLayout.SOUTH);
	}

	private JPanel buildControllsPanel() {
		SgLayout sgl = new SgLayout(2, 6, spacing(1), 2);
		sgl.setColSize(0, 1, SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setColSize(1, spacing(30), SgLayout.SIZE_TYPE_CONSTANT);

		JPanel ret = new JPanel(sgl);
		ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		int row = 0;

		// source file
		JPanel pnlTemp = new JPanel(new BorderLayout());
		pnlTemp.add(new JLabel(text("term.sourceFile")), BorderLayout.NORTH);
		ret.add(pnlTemp, sgl.cs(0, row));
		ret.add(lblSourceFilesSummaru = new JXLabel(), sgl.cs(1, row));
		lblSourceFilesSummaru.setLineWrap(true);

		// spacing
		row++;
		ret.add(buildEmptyLine(), sgl.cs(0, row, 2, 1));

		// recipients
		row++;
		pnlTemp = new JPanel(new BorderLayout());
		pnlTemp.add(new JLabel(text("term.recipients")), BorderLayout.NORTH);
		ret.add(pnlTemp, sgl.cs(0, row));
		ret.add(lblRecipientsSummary = new JXLabel(), sgl.cs(1, row));
		// ret.add(lblRecipientsSummary = new JXLabel(), sgl.cs(1, row, 1, 2));
		// row++;
		lblRecipientsSummary.setLineWrap(true);
		row++;
		ret.add(chkIgnoreMissingRecipients = new JCheckBox(), sgl.cs(1, row));

		// spacing
		row++;
		ret.add(buildEmptyLine(), sgl.cs(0, row, 2, 1));

		// after actions
		row++;
		ret.add(new JLabel(text("term.afterOperationPostAction")), sgl.cs(0, row));
		ret.add(chkDeleteSourceAfter = new JCheckBox("TBD"), sgl.cs(1, row));

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

		bindingContext.setupBinding(pm.getIsDeleteSourceAfter(), chkDeleteSourceAfter);

		bindingContext.setupBinding(pm.getSourceFilesSummary(), lblSourceFilesSummaru);
		bindingContext.setupBinding(pm.getRecipientsSummary(), lblRecipientsSummary);
		bindingContext.setupBinding(pm.getIsIgnoreMissingRecipientsWarning(), chkIgnoreMissingRecipients);
		bindingContext.registerPropertyValuePropagation(pm.getIsHasMissingRecipients(), chkIgnoreMissingRecipients,
				"enabled");

		bindingContext.registerPropertyValuePropagation(pm.getIsProgressVisible(), pbar, "visible");
		bindingContext.registerPropertyValuePropagation(pm.getProgressNote(), pbar, "string");
		bindingContext.registerPropertyValuePropagation(pm.getProgressValue(), pbar, "value");
		pbar.setVisible(false);

		bindingContext.registerOnChangeHandler(pm.getIsDisableControls(), isDisableControlsChanged);
		isDisableControlsChanged.handlePropertyChanged(this, null, false, pm.getIsDisableControls().getValue());

		bindingContext.setupBinding(pm.actionDoOperation, btnPerformOperation);
		bindingContext.setupBinding(pm.actionCancel, btnCancel);
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setLayout(new BorderLayout());
		ret.setResizable(false);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("encrypBackMany.action"));
		ret.add(pnl, BorderLayout.CENTER);
		ret.pack();
		UiUtils.centerWindow(ret);
		{
			// DIRTY-HACK: YES, FOLLOWING 2 LINES ARE REPEATED BY INTENTION,
			// OTHERWISE JXlABEL IS NOT
			// RETURNING CORRECT PREFERRED SIZE AND WHOLE LAYOUT IS SCREWED UP
			// THE ONLY WAY TO FORCE IT IS TO CENTER WINDOW. SO ONCE WE DID
			// FIRST TIME NOW WE CAN PACK AND CENTER AGAIN
			ret.pack();
			UiUtils.centerWindow(ret);
		}
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
