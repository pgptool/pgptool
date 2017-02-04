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
package org.pgptool.gui.ui.tools.linkbutton;

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