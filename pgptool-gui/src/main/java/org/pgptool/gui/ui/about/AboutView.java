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
package org.pgptool.gui.ui.about;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dialog.ModalityType;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.DialogViewBaseCustom;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.WindowIcon;

import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class AboutView extends DialogViewBaseCustom<AboutPm> {
	private JPanel pnl;
	private JButton btnClose;
	private JLabel lblVersion;
	private JLabel lblLinkToSite;
	private JLabel lblVersionStatus;
	private JLabel lblNewVersionLink;

	@Override
	protected void internalInitComponents() {
		SgLayout sgl = new SgLayout(2, 5, UiUtils.getFontRelativeSize(1), 2);
		sgl.setColSize(0, 64, SgLayout.SIZE_TYPE_CONSTANT);
		sgl.setColSize(1, UiUtils.getFontRelativeSize(20), SgLayout.SIZE_TYPE_CONSTANT);
		pnl = new JPanel(sgl);
		pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		ImageIcon imageIcon = new ImageIcon(WindowIcon.loadImage("/icons/icon-64.png"));
		JLabel lblIcon = new JLabel(imageIcon);
		pnl.add(lblIcon, sgl.cs(0, 0, 1, 4));

		pnl.add(new JLabel(UiUtils.plainToBoldHtmlString(Messages.get("term.version"))), sgl.cs(1, 0));
		JPanel pnlFlow = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 0));
		pnlFlow.setBorder(BorderFactory.createEmptyBorder(0, -5, 0, 0));
		pnlFlow.add(lblVersion = new JLabel());
		pnlFlow.add(lblVersionStatus = new JLabel());
		pnlFlow.add(lblNewVersionLink = new JLabel());
		initLinkAction(lblNewVersionLink, newVersionLinkClickListener);
		pnl.add(pnlFlow, sgl.cs(1, 1));

		pnl.add(new JLabel(UiUtils.plainToBoldHtmlString(Messages.get("term.linkToSite"))), sgl.cs(1, 2));
		pnl.add(lblLinkToSite = new JLabel(), sgl.cs(1, 3));
		initLinkAction(lblLinkToSite, siteLinkClickListener);

		JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		pnlButtons.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		pnl.add(pnlButtons, sgl.cs(0, 4, 2, 1));
		pnlButtons.add(btnClose = new JButton());
	}

	// TBD: Adopt LinkButton instead of this ridiculous construct
	private void initLinkAction(JLabel label, MouseListener mouseListener) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			return;
		}

		label.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		label.setForeground(Color.blue);
		// Font font = lblLinkToSite.getFont();
		// Map attributes = font.getAttributes();
		// attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		// lblLinkToSite.setFont(font.deriveFont(attributes));
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

			triggerAction();
		}

		protected abstract void triggerAction();
	}

	private MouseListener siteLinkClickListener = new LinkMouseListener() {
		@Override
		protected void triggerAction() {
			pm.actionOpenSite.actionPerformed(null);
		}
	};

	private MouseListener newVersionLinkClickListener = new LinkMouseListener() {
		@Override
		protected void triggerAction() {
			pm.actionDownloadNewVersion.actionPerformed(null);
		}
	};

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getVersion(), lblVersion);
		bindingContext.setupBinding(pm.getLinkToSite(), lblLinkToSite);
		bindingContext.setupBinding(pm.getVersionStatus(), lblVersionStatus);
		bindingContext.setupBinding(pm.getLinkToNewVersion(), lblNewVersionLink);

		bindingContext.setupBinding(pm.actionClose, btnClose);
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setLayout(new BorderLayout());
		ret.setResizable(false);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("term.aboutApp"));
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
		btnClose.getAction().actionPerformed(null);
	}
}
