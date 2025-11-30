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
package org.pgptool.gui.ui.tools.linkbutton;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.util.function.BiConsumer;
import javax.swing.Action;
import javax.swing.JLabel;
import org.pgptool.gui.ui.tools.UiUtils;
import ru.skarpushin.swingpm.bindings.HasAction;

public class LinkButton extends JLabel implements HasAction {
  @Serial private static final long serialVersionUID = 9012537495123322302L;

  public static final BiConsumer<LinkButton, Boolean> ENABLED_OR_DISABLED =
      new BiConsumer<>() {
        @Override
        public void accept(LinkButton t, Boolean enabled) {
          if (enabled) {
            t.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            t.setForeground(Color.blue);
          } else {
            t.setCursor(t.defaultCursor);
            t.setForeground(Color.gray);
          }
          t.superSetEnabled(enabled);
        }
      };

  public static final BiConsumer<LinkButton, Boolean> VISIBLE_OR_INVISIBLE =
      new BiConsumer<>() {
        @Override
        public void accept(LinkButton t, Boolean u) {
          t.setVisible(u);
        }
      };

  private Action action;
  private BiConsumer<LinkButton, Boolean> enabledBehavior = ENABLED_OR_DISABLED;
  private final Cursor defaultCursor;

  public LinkButton() {
    super();
    addMouseListener(filterClickListener);
    defaultCursor = getCursor();
    setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    setForeground(Color.blue);
  }

  @Override
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

  private final PropertyChangeListener listener =
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if ("enabled".equals(evt.getPropertyName())) {
            setEnabled((Boolean) evt.getNewValue());
          }
        }
      };

  private final MouseListener filterClickListener =
      new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          super.mouseClicked(e);
          if (e.getButton() != MouseEvent.BUTTON1) {
            return;
          }

          if (action != null) {
            action.actionPerformed(UiUtils.actionEvent(LinkButton.this, action));
          }
        }
      };

  @Override
  public void setEnabled(boolean enabled) {
    enabledBehavior.accept(LinkButton.this, enabled);
  }

  private void superSetEnabled(boolean enabled) {
    super.setEnabled(enabled);
  }

  public BiConsumer<LinkButton, Boolean> getEnabledBehavior() {
    return enabledBehavior;
  }

  public void setEnabledBehavior(BiConsumer<LinkButton, Boolean> enabledBehavior) {
    this.enabledBehavior = enabledBehavior;
  }
}
