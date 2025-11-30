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
package org.pgptool.gui.ui.keyslist;

import static org.pgptool.gui.ui.tools.swingpm.DialogViewBaseEx.spacing;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.ui.tools.geometrymemory.TableColumnsGeometryPersister;
import org.pgptool.gui.ui.tools.geometrymemory.TableColumnsGeometryPersisterImpl;
import org.pgptool.gui.ui.tools.swingpm.ViewBaseEx;
import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;

public class KeysTableView extends ViewBaseEx<KeysTablePm> {
  private static final String DELETE = "Delete";

  private final ScheduledExecutorService scheduledExecutorService;
  private final ConfigPairs uiGeom;

  private JPanel panelRoot;

  private JPanel panelTablePlaceholder;

  private JTable table;
  private JScrollPane scrollPane;
  private DefaultListSelectionModel selectionModel;
  private JPopupMenu ctxMenu;
  protected TableColumnsGeometryPersister tableColumnsGeometryPersister;

  private JLabel lblNoDataToDisplay;

  /** Code used to store and retrieve table layout */
  private String persistenceCode = "keyList";

  public KeysTableView(ScheduledExecutorService scheduledExecutorService, ConfigPairs uiGeom) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.uiGeom = uiGeom;
  }

  @Override
  protected void internalInitComponents() {
    panelRoot = new JPanel(new BorderLayout());
    initFormComponents();
    ctxMenu = new JPopupMenu();
  }

  private void initFormComponents() {
    panelTablePlaceholder = new JPanel(new BorderLayout());
    panelRoot.add(panelTablePlaceholder, BorderLayout.CENTER);
    panelTablePlaceholder.add(initTableComponent(), BorderLayout.CENTER);

    lblNoDataToDisplay = new JLabel(Messages.get("term.noDataToDisplay"));
    lblNoDataToDisplay.setHorizontalAlignment(JLabel.CENTER);
  }

  private JScrollPane initTableComponent() {
    table = new JTable();

    // Adjust some visual appearence
    table.setRowHeight(22);
    table.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

    // Add listeners
    selectionModel = new DefaultListSelectionModel();
    selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
    selectionModel.addListSelectionListener(rowSelectionListener);
    table.setSelectionModel(selectionModel);
    table.addMouseListener(listMouseListener);
    initTableKeyListener();

    // Envelope in scrollpane
    scrollPane = new JScrollPane();
    scrollPane.addMouseListener(listMouseListener);
    scrollPane.setViewportView(table);
    return scrollPane;
  }

  private void initTableKeyListener() {
    int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
    InputMap inputMap = table.getInputMap(condition);
    ActionMap actionMap = table.getActionMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE);
    actionMap.put(
        DELETE,
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (pm == null) {
              return;
            }
            safePerformAction(pm.getActionDelete(), null);
          }
        });
  }

  final MouseAdapter listMouseListener =
      new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          if (e.isPopupTrigger()) {
            myPopupEvent(e);
          }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          if (e.isPopupTrigger()) {
            myPopupEvent(e);
          }
        }

        private void myPopupEvent(MouseEvent e) {
          if (!isAttached()) {
            return;
          }

          if (e.getComponent() == table) {
            int r = table.rowAtPoint(e.getPoint());
            if (r >= 0 && r < table.getRowCount() && pm.getKeys().findRowByIdx(r) != null) {
              table.setRowSelectionInterval(r, r);
            } else {
              table.clearSelection();
            }
          } else {
            table.clearSelection();
          }

          if (ctxMenu.getComponentCount() > 0) {
            ctxMenu.show(e.getComponent(), e.getX(), e.getY());
          }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getComponent() == table
              && e.getClickCount() == 2
              && e.getButton() == MouseEvent.BUTTON1) {
            safePerformAction(pm.getActionForRowDoubleClick(), null);
          }
        }
      };

  private void safePerformAction(Action action, ActionEvent event) {
    if (action != null && action.isEnabled()) {
      action.actionPerformed(event);
    }
  }

  protected final ListSelectionListener rowSelectionListener =
      new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (e.getValueIsAdjusting() || !isAttached()) {
            return;
          }

          Key selectedRow = getSelectedRow();
          if (selectedRow == null) {
            table.clearSelection();
          }
          if (pm.getSelectedRow() != null) {
            pm.getSelectedRow().setValue(selectedRow);
          }
        }
      };

  private Key getSelectedRow() {
    int row = table.getSelectedRow();
    if (row < 0) {
      return null;
    }
    return pm.getKeys().findRowByIdx(row);
  }

  private final TypedPropertyChangeListener<Key> rowPmSelectionListener =
      new TypedPropertyChangeListener<>() {
        @Override
        public void handlePropertyChanged(
            Object source, String propertyName, Key oldValue, Key newValue) {
          if (newValue == getSelectedRow()) {
            return;
          }

          if (newValue == null) {
            table.clearSelection();
            return;
          }

          int idx = pm.getKeys().indexOf(newValue);
          if (idx < 0) {
            log.warn(
                "Asked to select nonexistent record {}. Skipping selection request.", newValue);
            return;
          }
          table.setRowSelectionInterval(idx, idx);
        }
      };

  @Override
  protected void internalBindToPm() {
    super.internalBindToPm();

    if (pm.getHasData() != null) {
      hasDataChangeHandler.handlePropertyChanged(
          this, pm.getHasData().getPropertyName(), false, pm.getHasData().getValue());
      bindingContext.registerOnChangeHandler(pm.getHasData(), hasDataChangeHandler);
    } else {
      hasDataChangeHandler.handlePropertyChanged(this, "hasData", false, true);
    }

    if (pm.getSelectedRow() != null) {
      bindingContext.registerOnChangeHandler(pm.getSelectedRow(), rowPmSelectionListener);
    }

    bindToActions();
  }

  private void bindToActions() {
    if (pm.getContextMenuActions() == null) {
      return;
    }
    for (Action action : pm.getContextMenuActions()) {
      if (action == null) {
        ctxMenu.addSeparator();
      } else {
        ctxMenu.add(action);
      }
    }
  }

  private final TypedPropertyChangeListener<Boolean> hasDataChangeHandler =
      new TypedPropertyChangeListener<>() {
        @Override
        public void handlePropertyChanged(
            Object source, String propertyName, Boolean oldValue, Boolean newValue) {
          if (!newValue) {
            table.setModel(new DefaultTableModel());
            panelTablePlaceholder.removeAll();
            panelTablePlaceholder.add(lblNoDataToDisplay, BorderLayout.CENTER);
            panelTablePlaceholder.revalidate();
            panelTablePlaceholder.repaint();
            return;
          }

          if (tableColumnsGeometryPersister != null) {
            tableColumnsGeometryPersister.detach();
            tableColumnsGeometryPersister = null;
          }
          table.setModel(pm.getKeys());
          adjustColumnsWidths();
          tableColumnsGeometryPersister =
              new TableColumnsGeometryPersisterImpl(
                  table, persistenceCode, uiGeom, scheduledExecutorService);
          tableColumnsGeometryPersister.restoreColumnsConfig();

          table.repaint();
          panelTablePlaceholder.removeAll();
          panelTablePlaceholder.add(scrollPane, BorderLayout.CENTER);
          panelTablePlaceholder.revalidate();
          panelTablePlaceholder.repaint();
        }
      };

  @Override
  protected void internalUnbindFromPm() {
    super.internalUnbindFromPm();
    table.setModel(new DefaultTableModel());
    ctxMenu.removeAll();
  }

  private void adjustColumn(
      DefaultTableCellRenderer leftRenderer,
      int columnIdx,
      int columnSize,
      DefaultTableCellRenderer cellRenderer) {
    table.getColumnModel().getColumn(columnIdx).setPreferredWidth(columnSize);
    table.getColumnModel().getColumn(columnIdx).setCellRenderer(cellRenderer);
  }

  private void adjustColumnsWidths() {
    DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
    leftRenderer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    leftRenderer.setHorizontalAlignment(JLabel.LEFT);

    DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
    rightRenderer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
    rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
    centerRenderer.setHorizontalAlignment(JLabel.CENTER);

    adjustColumn(leftRenderer, KeysTableModel.COLUMN_USER, spacing(30), leftRenderer);
    adjustColumn(leftRenderer, KeysTableModel.COLUMN_KEY_ID, spacing(16), centerRenderer);
    adjustColumn(leftRenderer, KeysTableModel.COLUMN_KEY_TYPE, spacing(8), centerRenderer);
    adjustColumn(leftRenderer, KeysTableModel.COLUMN_ALGORITHM, spacing(13), centerRenderer);
    adjustColumn(leftRenderer, KeysTableModel.COLUMN_CREATED_ON, spacing(10), centerRenderer);
    adjustColumn(leftRenderer, KeysTableModel.COLUMN_EXPIRES_AT, spacing(10), centerRenderer);
  }

  @Override
  protected void internalRenderTo(Container owner, Object constraints) {
    owner.add(panelRoot, constraints);
  }

  @Override
  protected void internalUnrender() {
    panelRoot.getParent().remove(panelRoot);
  }

  public String getPersistenceCode() {
    return persistenceCode;
  }

  public void setPersistenceCode(String persistenceCode) {
    this.persistenceCode = persistenceCode;
  }
}
