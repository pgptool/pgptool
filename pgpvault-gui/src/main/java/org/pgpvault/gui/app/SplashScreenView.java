package org.pgpvault.gui.app;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class SplashScreenView extends JFrame {
	private static final long serialVersionUID = -1059755470997596254L;

	public SplashScreenView() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				initComponents();
			}
		});
	}

	private void initComponents() {
		setSize(300, 50);
		setLayout(new BorderLayout());
		centerWindow(this);
		setResizable(false);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setUndecorated(true);
		JLabel label;
		add(label = new JLabel("Loading..."));
		label.setHorizontalAlignment(JLabel.CENTER);
		setVisible(true);
	}

	private void centerWindow(JFrame frm) {
		Dimension scrDim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (scrDim.width - frm.getSize().width) / 2;
		int y = (scrDim.height - frm.getSize().height) / 2;
		frm.setLocation(x, y);
	}

	public void close() {
		this.setVisible(false);
		this.dispose();
	}
}
