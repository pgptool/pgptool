package org.pgptool.gui.ui.encryptbackmultiple;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableModel;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.geometrymemory.TableColumnsGeometryPersister;
import org.pgptool.gui.ui.tools.geometrymemory.TableColumnsGeometryPersisterImpl;
import org.pgptool.gui.ui.tools.swingpm.DialogViewBaseEx;
import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;

public class EncryptMultipleResultsDialogView
    extends DialogViewBaseEx<EncryptMultipleResultsDialogPm> {

  private JPanel panelRoot;
  private JTable table;
  private JScrollPane scrollPane;
  private JButton btnClose;
  private JLabel lblSummary;
  private TableColumnsGeometryPersister tableColumnsGeometryPersister;

  private final TypedPropertyChangeListener<TableModel> modelChanged =
      new TypedPropertyChangeListener<>() {
        @Override
        public void handlePropertyChanged(
            Object source, String propertyName, TableModel oldValue, TableModel newValue) {
          table.setModel(newValue);
          // Resize first column wider
          if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setPreferredWidth(400);
            table.getColumnModel().getColumn(1).setPreferredWidth(80);
            table.getColumnModel().getColumn(2).setPreferredWidth(400);
          }
        }
      };

  @Override
  protected void internalInitComponents() {
    panelRoot = new JPanel(new BorderLayout());
    panelRoot.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

    table = new JTable();
    table.setFillsViewportHeight(true);
    table.addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
              onDoubleClick(e);
            }
          }
        });
    scrollPane = new JScrollPane(table);
    panelRoot.add(scrollPane, BorderLayout.CENTER);

    JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    south.add(btnClose = new JButton());
    panelRoot.add(south, BorderLayout.SOUTH);
  }

  private void onDoubleClick(MouseEvent e) {
    int row = table.getSelectedRow();
    if (row < 0) {
      return;
    }
    TableModel tm = table.getModel();
    Object rowObj = null;
    try {
      rowObj = tm.getValueAt(row, -1);
    } catch (Throwable ignore) {
      // model might not support -1 column
    }

    if (rowObj instanceof EncryptMultipleResultsTableModel.Row r) {
      if (r.error != null) {
        // Show full error message
        EntryPoint.reportExceptionToUser(
            UiUtils.actionEvent(table, "EncryptMultipleResultsDialog.doubleClick"),
            "exception.unexpected",
            r.error);
      }
    }
  }

  @Override
  protected void internalBindToPm() {
    super.internalBindToPm();
    bindingContext.setupBinding(pm.getActionClose(), btnClose);
    bindingContext.registerOnChangeHandler(pm.getRowsTableModel(), modelChanged);
    // apply initial model immediately
    modelChanged.handlePropertyChanged(
        this,
        "rowsTableModel",
        null,
        pm.getRowsTableModel() == null ? null : pm.getRowsTableModel().getValue());
  }

  @Override
  protected JDialog initDialog(Window owner, Object constraints) {
    JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
    ret.setLayout(new BorderLayout());
    ret.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    ret.setResizable(true);
    ret.setTitle(Messages.get("encrypBackMany.encryptBackAll.results"));
    ret.add(panelRoot, BorderLayout.CENTER);

    // Reasonable initial size; geometry persister will restore saved size
    ret.setMinimumSize(new Dimension(1200, 500));
    ret.pack();

    initWindowGeometryPersister(ret, "EncryptMultipleResultsDialog");

    // Persist table columns geometry
    tableColumnsGeometryPersister =
        new TableColumnsGeometryPersisterImpl(
            table, "EncryptMultipleResultsDialog.table", uiGeom, scheduledExecutorService);
    tableColumnsGeometryPersister.restoreColumnsConfig();

    UiUtils.centerWindow(ret, owner);
    return ret;
  }

  @Override
  protected JPanel getRootPanel() {
    return panelRoot;
  }

  @Override
  protected void handleDialogShown() {
    super.handleDialogShown();
    dialog.getRootPane().setDefaultButton(btnClose);
  }

  @Override
  protected void dispatchWindowCloseEvent(ActionEvent originAction) {
    btnClose.getAction().actionPerformed(originAction);
  }
}
