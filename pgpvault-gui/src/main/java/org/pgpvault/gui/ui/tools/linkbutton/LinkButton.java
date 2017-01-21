package org.pgpvault.gui.ui.tools.linkbutton;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JLabel;

public class LinkButton extends JLabel {
	private static final long serialVersionUID = 9012537495123322302L;

	private Action action;
	private Cursor prevCursor;

	public LinkButton() {
		super();
		addMouseListener(filterClickListener);
	}

	public void setAction(Action newAction) {
		if (action != null) {
			action.removePropertyChangeListener(listener);
		}

		action = newAction;

		if (newAction != null) {
			setEnabled(newAction.isEnabled());
			newAction.addPropertyChangeListener(listener);
			setText((String) action.getValue(Action.NAME));
		} else {
			setEnabled(false);
		}
	}

	private PropertyChangeListener listener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if ("enabled".equals(evt.getPropertyName())) {
				LinkButton.this.setEnabled((Boolean) evt.getNewValue());
			}
		}
	};

	private MouseListener filterClickListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			super.mouseClicked(e);
			if (e.getButton() != MouseEvent.BUTTON1) {
				return;
			}

			if (action != null) {
				action.actionPerformed(null);
			}
		}
	};

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			prevCursor = getCursor();
			setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
			setForeground(Color.blue);
		} else {
			prevCursor = getCursor();
			setCursor(prevCursor);
			setForeground(Color.gray);
		}
	};
}