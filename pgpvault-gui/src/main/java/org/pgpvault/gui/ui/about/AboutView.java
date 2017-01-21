package org.pgpvault.gui.ui.about;

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

import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.ui.tools.DialogViewBaseCustom;
import org.pgpvault.gui.ui.tools.UiUtils;
import org.pgpvault.gui.ui.tools.WindowIcon;

import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class AboutView extends DialogViewBaseCustom<AboutPm> {
	private JPanel pnl;
	private JButton btnClose;
	private JLabel lblVersion;
	private JLabel lblLinkToSite;

	@Override
	protected void internalInitComponents() {
		SgLayout sgl = new SgLayout(2, 5, UiUtils.getFontRelativeSize(1), 2);
		sgl.setColSize(0, 64, SgLayout.SIZE_TYPE_CONSTANT);
		sgl.setColSize(1, UiUtils.getFontRelativeSize(20), SgLayout.SIZE_TYPE_CONSTANT);
		pnl = new JPanel(sgl);
		pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		ImageIcon imageIcon = new ImageIcon(WindowIcon.loadImage("/icon-64.png"));
		JLabel lblIcon = new JLabel(imageIcon);
		pnl.add(lblIcon, sgl.cs(0, 0, 1, 4));

		pnl.add(new JLabel(UiUtils.plainToBoldHtmlString(Messages.get("term.version"))), sgl.cs(1, 0));
		pnl.add(lblVersion = new JLabel(), sgl.cs(1, 1));

		pnl.add(new JLabel(UiUtils.plainToBoldHtmlString(Messages.get("term.linkToSite"))), sgl.cs(1, 2));
		pnl.add(lblLinkToSite = new JLabel(), sgl.cs(1, 3));
		initSiteLink();

		JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		pnlButtons.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
		pnl.add(pnlButtons, sgl.cs(0, 4, 2, 1));
		pnlButtons.add(btnClose = new JButton());
	}

	private void initSiteLink() {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			return;
		}

		lblLinkToSite.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
		lblLinkToSite.setForeground(Color.blue);
		// Font font = lblLinkToSite.getFont();
		// Map attributes = font.getAttributes();
		// attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
		// lblLinkToSite.setFont(font.deriveFont(attributes));
		lblLinkToSite.addMouseListener(siteLinkClickListener);
	}

	private MouseListener siteLinkClickListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			super.mouseClicked(e);
			if (!isAttached()) {
				return;
			}

			if (e.getButton() != MouseEvent.BUTTON1) {
				return;
			}

			pm.actionOpenSite.actionPerformed(null);
		}
	};

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getVersion(), lblVersion);
		bindingContext.setupBinding(pm.getLinkToSite(), lblLinkToSite);

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
