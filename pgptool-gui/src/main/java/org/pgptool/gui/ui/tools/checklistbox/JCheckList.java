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
package org.pgptool.gui.ui.tools.checklistbox;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExEventListener;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.lists.ModelListProperty;

/**
 * This is an extension of JList which can act as a check list box. It's quite specific to this app
 * (and usage of swingpm library)
 *
 * @author sergeyk
 */
public class JCheckList<E> extends JList<E> {
  @Serial private static final long serialVersionUID = 5763129241133814837L;
  protected ListEx<E> checkState;
  protected boolean skipEventsFromListEx;

  /**
   * NOTE: This is used to make Shift+Click work (copy selection state). This collection used to
   * track selections in list. Not the most elegant way, but getting things done.
   *
   * <p>TBD: Maybe refactor it someday
   */
  private final List<Integer> recentSelection = new ArrayList<>();

  public JCheckList() {
    super();
    setCheckState(null);

    setCellRenderer(new CheckListCellRenderer<>(x -> getCheckState().contains(x)));
    addListSelectionListener(selectionTracker);
    addMouseListener(mouseListener);
    addSpaceActionhandler(this);

    // TBD: Add support for "dragging functionality" AND shift + key up+down

    // NOTE: ALthough this is multi check list box, for Swing it's a single check
    // listbox
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  }

  protected final ListSelectionListener selectionTracker =
      new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting()) {
            return;
          }

          if (getSelectedIndex() < 0) {
            recentSelection.clear();
            return;
          }
          recentSelection.add(getSelectedIndex());

          while (recentSelection.size() > 2) {
            recentSelection.remove(0);
          }
        }
      };

  private void addSpaceActionhandler(JComponent host) {
    InputMap inputMap = host.getInputMap();
    ActionMap actionMap = host.getActionMap();
    KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
    String key = keyStroke.toString();
    inputMap.put(keyStroke, key);
    actionMap.put(key, invertCheckAction);
  }

  protected final AbstractAction invertCheckAction =
      new AbstractAction() {
        @Serial private static final long serialVersionUID = -6690198260921289877L;

        @Override
        public void actionPerformed(ActionEvent e) {
          int selected = getSelectedIndex();
          if (selected >= 0) {
            invertItemCheckedState(selected);
          }
        }
      };

  protected final MouseAdapter mouseListener =
      new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (!isEnabled()) {
            return;
          }

          int index = locationToIndex(e.getPoint());
          if (index < 0) {
            return;
          }

          int size = recentSelection.size();
          if (size >= 2 && e.isShiftDown()) {
            extendItemSelection(recentSelection.get(size - 2), recentSelection.get(size - 1));
          } else {
            invertItemCheckedState(index);
          }
        }

        private void extendItemSelection(int index, int curSel) {
          E element = getModel().getElementAt(index);
          boolean newCheckState = checkState.contains(element);
          int from = Math.min(curSel, index);
          int to = Math.max(curSel, index);

          skipEventsFromListEx = true;
          try {
            for (int i = from; i <= to; i++) {
              element = getModel().getElementAt(i);
              if (newCheckState) {
                if (!checkState.contains(element)) {
                  checkState.add(element);
                }
              } else {
                checkState.remove(element);
              }
            }
          } finally {
            skipEventsFromListEx = false;
          }

          Rectangle rect = getCellBounds(from, to);
          repaint(rect);
        }
      };

  private void invertItemCheckedState(int index) {
    skipEventsFromListEx = true;
    try {
      E element = getModel().getElementAt(index);
      boolean oldCheckState = checkState.contains(element);
      if (oldCheckState) {
        checkState.remove(element);
      } else {
        checkState.add(element);
      }
    } finally {
      skipEventsFromListEx = false;
    }

    // re-draw this item
    Rectangle rect = getCellBounds(index, index);
    repaint(rect);
  }

  private final ListExEventListener<E> checkStateListener =
      new ListExEventListener<>() {
        @Override
        public void onItemAdded(E item, int atIndex) {
          repaintRowForItem(item);
        }

        @Override
        public void onItemChanged(E item, int atIndex) {
          repaintRowForItem(item);
        }

        @Override
        public void onItemRemoved(E item, int wasAtIndex) {
          repaintRowForItem(item);
        }

        @Override
        public void onAllItemsRemoved(int sizeWas) {
          if (skipEventsFromListEx) {
            return;
          }
          repaint();
        }

        private void repaintRowForItem(E item) {
          if (skipEventsFromListEx) {
            return;
          }

          int elementIdx = tryResolveLocalIndex(item);
          if (elementIdx == -1) {
            return;
          }
          Rectangle rect = getCellBounds(elementIdx, elementIdx);
          repaint(rect);
        }

        @SuppressWarnings("unchecked")
        private int tryResolveLocalIndex(E item) {
          if (getModel() instanceof ModelListProperty) {
            return ((ModelListProperty<E>) getModel()).getList().indexOf(item);
          }
          return -1;
        }
      };

  public ListEx<E> getCheckState() {
    return checkState;
  }

  public void setCheckState(ListEx<E> checkState) {
    if (this.checkState != null) {
      this.checkState.removeListExEventListener(checkStateListener);
    }

    this.checkState = checkState != null ? checkState : new ListExImpl<>();
    repaint();

    if (checkState == null) {
      return;
    }

    checkState.addListExEventListener(checkStateListener);
  }
}
