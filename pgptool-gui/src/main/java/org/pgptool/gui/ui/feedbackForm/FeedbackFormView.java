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
package org.pgptool.gui.ui.feedbackForm;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.pgptool.gui.ui.tools.ControlsDisabler;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class FeedbackFormView extends DialogViewBaseCustom<FeedbackFormPm> {
	private JButton btnSubmit;
	private JButton btnCancel;

	private JLabel lblConnectingServer;

	private JPanel root;
	private JPanel controls;
	private JTextArea edFeedback;
	private JTextField edEmail;

	private List<JRadioButton> rating = new ArrayList<>();
	private ControlsDisabler controlsBlocker;

	@Override
	protected void internalInitComponents() {
		// Root panel
		root = new JPanel(new BorderLayout());

		// Checking connectivity
		lblConnectingServer = new JLabel(text("feedback.connectingServer"), JLabel.CENTER);
		lblConnectingServer.setBorder(BorderFactory.createEmptyBorder(50, 0, 0, 0));
		root.add(lblConnectingServer, BorderLayout.NORTH);
		// lblTestingConnection.setMaxLineSpan(UiUtils.getFontRelativeSize(30));

		controls = buildControlsPanel();
		root.add(controls, BorderLayout.CENTER);

		// buttons
		root.add(buildButtonsPanel(), BorderLayout.SOUTH);
	}

	private JPanel buildButtonsPanel() {
		JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		pnlButtons.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		pnlButtons.add(btnSubmit = new JButton());
		pnlButtons.add(btnCancel = new JButton());
		return pnlButtons;
	}

	private JPanel buildControlsPanel() {
		// Main controls panel
		SgLayout sgl = new SgLayout(1, 6, UiUtils.getFontRelativeSize(1), 2);
		sgl.setColSize(0, UiUtils.getFontRelativeSize(20), SgLayout.SIZE_TYPE_WEIGHTED);
		sgl.setRowSize(3, UiUtils.getFontRelativeSize(10), SgLayout.SIZE_TYPE_WEIGHTED);
		JPanel ret = new JPanel(sgl);
		ret.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// rating
		int row = 0;
		ret.add(new JLabel(text("term.rating")), sgl.cs(0, row));
		row++;
		JPanel ratingPanel = new JPanel();
		for (int i = 1; i <= 10; i++) {
			JRadioButton radioButton = new JRadioButton("" + i, i == FeedbackFormPm.DEFAULT_RATING);
			rating.add(radioButton);
			radioButton.addActionListener(buildRadioBUttonActionListener(radioButton));
			ratingPanel.add(radioButton);
		}

		ret.add(ratingPanel, sgl.cs(0, row));
		row++;

		// new release notes
		ret.add(new JLabel(text("term.feedback")), sgl.cs(0, row));
		row++;
		ret.add(new JScrollPane(edFeedback = new JTextArea()), sgl.cs(0, row));
		edFeedback.setFont(new JTextField().getFont());
		edFeedback.setLineWrap(true);
		edFeedback.setMargin(new Insets(5, 5, 5, 5));
		row++;

		// email
		ret.add(new JLabel(text("term.email")), sgl.cs(0, row));
		row++;
		ret.add(edEmail = new JTextField(), sgl.cs(0, row));
		row++;

		controlsBlocker = new ControlsDisabler(ret);
		return ret;
	}

	private ActionListener buildRadioBUttonActionListener(JRadioButton radioButton) {
		return new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int idx = rating.indexOf(radioButton);
				for (int i = 0; i < rating.size(); i++) {
					if (i == idx) {
						pm.getRating().setValue(i + 1);
						rating.get(i).setSelected(true);
					} else {
						rating.get(i).setSelected(false);
					}
				}
			}
		};
	}

	@Override
	protected void handleDialogShown() {
		super.handleDialogShown();
		edFeedback.requestFocusInWindow();
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getFeedback(), edFeedback);
		bindingContext.setupBinding(pm.getEmail(), edEmail);

		bindingContext.registerPropertyValuePropagation(pm.getShowConnectingToServerStatus(), lblConnectingServer,
				"visible");
		bindingContext.registerPropertyValuePropagation(pm.getShowFeedbackFrom(), controls, "visible");
		bindingContext.registerOnChangeHandler(pm.getFeedbackFormDisabled(), controlsBlocker);

		bindingContext.setupBinding(pm.actionSubmit, btnSubmit);
		bindingContext.setupBinding(pm.actionCancel, btnCancel);
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.MODELESS);
		ret.setLayout(new BorderLayout());

		ret.setResizable(true);
		ret.setMinimumSize(new Dimension(UiUtils.getFontRelativeSize(50), UiUtils.getFontRelativeSize(30)));

		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(text("action.leaveFeedback"));
		ret.add(root, BorderLayout.CENTER);
		ret.getRootPane().setDefaultButton(btnSubmit);

		initWindowGeometryPersister(ret, "fbackFrm");

		return ret;
	}

	@Override
	protected JPanel getRootPanel() {
		return root;
	}

	@Override
	protected void dispatchWindowCloseEvent() {
		btnCancel.getAction().actionPerformed(null);
	}
}
