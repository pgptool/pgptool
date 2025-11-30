package org.pgptool.gui.ui.encryptbackmultiple;

import static org.pgptool.gui.app.Messages.text;

import com.google.common.base.Preconditions;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.Action;
import javax.swing.table.TableModel;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.ui.encryptbackmultiple.EncryptBackMultiplePm.BatchEncryptionResult;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import ru.skarpushin.swingpm.base.DialogViewBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class EncryptMultipleResultsDialogPm
    extends PresentationModelBaseEx<Void, BatchEncryptionResult> {

  private ModelProperty<TableModel> rowsTableModel;

  private final Action actionClose =
      new LocalizedActionEx("action.close", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          // just close the dialog
          for (DialogViewBase<?> v : findViewsWhichImplements(DialogViewBase.class)) {
            v.unrender();
          }
        }
      };

  @Override
  public boolean init(ActionEvent originAction, Void host, BatchEncryptionResult initParams) {
    super.init(originAction, host, initParams);
    Preconditions.checkNotNull(initParams, "initParams required");

    rowsTableModel = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "rowsTableModel");
    rowsTableModel.setValueByOwner(buildTableModel(initParams));
    return true;
  }

  private TableModel buildTableModel(BatchEncryptionResult ret) {
    List<EncryptMultipleResultsTableModel.Row> rows = new ArrayList<>();
    Set<String> added = new HashSet<>();

    for (EncryptBackResult resultType : ret.categories.keySet()) {
      for (String file : ret.categories.get(resultType)) {
        if (!added.add(file)) {
          continue;
        }
        switch (resultType) {
          case Encrypted:
            rows.add(
                new EncryptMultipleResultsTableModel.Row(file, text("term.Encrypted"), "", null));
            break;
          case ConcurrentChangeDetected:
            rows.add(
                new EncryptMultipleResultsTableModel.Row(
                    file,
                    text("term.attention"),
                    text("encrBackAll.ConcurrentChangeDetected"),
                    null));
            break;
          case Exception:
            Throwable t = ret.errors.get(file);
            String msg =
                t == null
                    ? Messages.get("encrBackAll.Exception")
                    : ConsoleExceptionUtils.getAllMessages(t);
            rows.add(new EncryptMultipleResultsTableModel.Row(file, text("term.Error"), msg, t));
            break;
          default:
            String reason = Messages.get("encrBackAll." + resultType.name());
            rows.add(
                new EncryptMultipleResultsTableModel.Row(file, text("term.Skipped"), reason, null));
            break;
        }
      }
    }

    // In case there are errors without category (like OPERATION)
    for (Map.Entry<String, Throwable> e : ret.errors.entrySet()) {
      if (added.contains(e.getKey())) continue;
      String msg = ConsoleExceptionUtils.getAllMessages(e.getValue());
      rows.add(
          new EncryptMultipleResultsTableModel.Row(
              e.getKey(), text("term.Error"), msg, e.getValue()));
    }

    return new EncryptMultipleResultsTableModel(rows);
  }

  public ModelPropertyAccessor<TableModel> getRowsTableModel() {
    return rowsTableModel.getModelPropertyAccessor();
  }

  public Action getActionClose() {
    return actionClose;
  }
}
