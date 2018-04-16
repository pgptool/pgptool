package org.pgptool.gui.hintsforusage.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXLabel;
import org.pgptool.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.base.ViewBase;

public class HintView extends ViewBase<HintPm> {
	private JPanel root;

	private JXLabel message;
	private JPanel pnlButtons;

	@Override
	protected void internalInitComponents() {
		int charHalfWidth = UiUtils.getFontRelativeSize(1) / 2;

		root = new JPanel(new BorderLayout());
		root.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

		JPanel pnlPadding = new JPanel(new BorderLayout());
		root.add(pnlPadding);
		pnlPadding.setBorder(BorderFactory.createLineBorder(Color.yellow.darker(), 1, true));

		JPanel pnlMsg = new JPanel(new BorderLayout());
		pnlMsg.setOpaque(false);
		pnlPadding.add(pnlMsg);
		pnlMsg.setBorder(BorderFactory.createEmptyBorder(charHalfWidth / 2, 5, charHalfWidth / 2, 5));
		message = new JXLabel();
		message.setLineWrap(true);
		message.setOpaque(false);
		pnlMsg.add(message);

		pnlButtons = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		pnlButtons.setOpaque(false);
		// JButton btnOk = new JButton("Tell me more");
		// pnlButtons.add(btnOk);
		// JButton btnCancel = new JButton("Snooze");
		// pnlButtons.add(btnCancel);
		pnlMsg.add(pnlButtons, BorderLayout.EAST);

		// gentle green
		// pnlPadding.setBackground(new Color(173, 252, 135));

		// super-light green
		pnlPadding.setBackground(new Color(221, 253, 191));

		// orange envelop color
		// pnlPadding.setBackground(new Color(255, 120, 37));
		// message.setForeground(Color.WHITE);
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		bindingContext.setupBinding(pm.getMessage(), message);

		// add buttons
		for (Action action : pm.getActions()) {
			JButton btn = new JButton(action);
			pnlButtons.add(btn);
		}
	}

	@Override
	protected void internalUnbindFromPm() {
		super.internalUnbindFromPm();
		pnlButtons.removeAll();
	}

	@Override
	protected void internalRenderTo(Container owner, Object constraints) {
		owner.add(root, constraints);
	}

	@Override
	protected void internalUnrender() {
		root.getParent().remove(root);
	}
}
