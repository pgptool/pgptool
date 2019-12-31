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
package org.pgptool.gui.ui.checkForUpdates;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class CheckForUpdatesView extends DialogViewBaseCustom<CheckForUpdatesPm> {
	private JPanel pnl;
	private JButton btnDownload;
	private JButton btnSnoozeVersion;
	private JButton btnClose;
	private JLabel lblVersion;
	private JLabel lblVersionStatus;
	private JLabel lblNewVersionLink;
	private JLabel lblNewVersionTitle;
	private JTextArea lblNewVersionReleaseNotes;

	@Override
	protected void internalInitComponents() {
		SgLayout sgl = new SgLayout(2, 5, UiUtils.getFontRelativeSize(1), 2);
		sgl.setColSize(1, UiUtils.getFontRelativeSize(30), SgLayout.SIZE_TYPE_CONSTANT);
		sgl.setRowSize(3, UiUtils.getFontRelativeSize(9), SgLayout.SIZE_TYPE_CONSTANT);
		pnl = new JPanel(sgl);
		pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// version
		int row = 0;
		pnl.add(new JLabel(text("term.version")), sgl.cs(0, row));
		JPanel pnlFlow = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
		pnlFlow.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));
		pnlFlow.add(lblVersion = new JLabel());
		pnlFlow.add(lblVersionStatus = new JLabel());
		pnlFlow.add(lblNewVersionLink = new JLabel());
		initSiteLink(lblNewVersionLink, newVersionLinkClickListener);
		pnl.add(pnlFlow, sgl.cs(1, row));
		row++;

		// new title
		pnl.add(new JLabel(text("term.newVersionTitle")), sgl.cs(0, row));
		pnl.add(lblNewVersionTitle = new JLabel(), sgl.cs(1, row));
		row++;

		// new release notes
		pnl.add(new JLabel(text("term.newVersionReleaseNotes")), sgl.cs(0, row, 2, 1));
		row++;
		pnl.add(new JScrollPane(lblNewVersionReleaseNotes = new JTextArea()), sgl.cs(0, row, 2, 1));
		lblNewVersionReleaseNotes.setLineWrap(true);
		lblNewVersionReleaseNotes.setWrapStyleWord(true);
		lblNewVersionReleaseNotes.setMargin(new Insets(5, 5, 5, 5));
		lblNewVersionReleaseNotes.setEditable(false);
		lblNewVersionReleaseNotes.setFont(new JTextField().getFont());
		row++;

		// buttons
		JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
		pnlButtons.setBorder(BorderFactory.createEmptyBorder(6, -5, 0, 0));
		pnl.add(pnlButtons, sgl.cs(0, row, 2, 1));
		pnlButtons.add(btnDownload = new JButton());
		pnlButtons.add(btnSnoozeVersion = new JButton());
		pnlButtons.add(btnClose = new JButton());
	}

	private void initSiteLink(JLabel label, MouseListener mouseListener) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			return;
		}

		label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		label.setForeground(Color.blue);
		label.addMouseListener(mouseListener);
	}

	private abstract class LinkMouseListener extends MouseAdapter implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent e) {
			super.mouseClicked(e);
			if (!isAttached()) {
				return;
			}

			if (e.getButton() != MouseEvent.BUTTON1) {
				return;
			}

			triggerAction(UiUtils.actionEvent(e.getSource(), "LinkActivated"));
		}

		protected abstract void triggerAction(ActionEvent originEvent);
	}

	private MouseListener newVersionLinkClickListener = new LinkMouseListener() {
		@Override
		protected void triggerAction(ActionEvent originEvent) {
			pm.actionDownloadNewVersion.actionPerformed(originEvent);
		}
	};

	private TypedPropertyChangeListener<String> onReleaseNotesChanged = new TypedPropertyChangeListener<String>() {
		@Override
		public void handlePropertyChanged(Object source, String propertyName, String oldValue, String newValue) {
			lblNewVersionReleaseNotes.setText(newValue == null ? "" : newValue);
			// TBD: Move this line into PropertyBinder into SwingPM
			lblNewVersionReleaseNotes.select(0, 0);
		}
	};

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getCurrentVersion(), lblVersion);
		bindingContext.setupBinding(pm.getVersionCheckStatus(), lblVersionStatus);
		bindingContext.setupBinding(pm.getLinkToNewVersion(), lblNewVersionLink);

		bindingContext.setupBinding(pm.getNewVersionTitle(), lblNewVersionTitle);
		bindingContext.registerOnChangeHandler(pm.getNewVersionReleaseNotes(), onReleaseNotesChanged);
		onReleaseNotesChanged.handlePropertyChanged(pm, pm.getNewVersionReleaseNotes().getPropertyName(), "",
				pm.getNewVersionReleaseNotes().getValue());

		bindingContext.setupBinding(pm.actionDownloadNewVersion, btnDownload);
		bindingContext.setupBinding(pm.actionSnoozeVersion, btnSnoozeVersion);
		bindingContext.setupBinding(pm.actionClose, btnClose);
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setLayout(new BorderLayout());
		ret.setResizable(false);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(text("action.checkForUpdates"));
		ret.add(pnl, BorderLayout.CENTER);
		ret.pack();
		UiUtils.centerWindow(ret, owner);
		return ret;
	}

	@Override
	protected JPanel getRootPanel() {
		return pnl;
	}

	@Override
	protected void dispatchWindowCloseEvent(ActionEvent originAction) {
		btnClose.getAction().actionPerformed(originAction);
	}
}
