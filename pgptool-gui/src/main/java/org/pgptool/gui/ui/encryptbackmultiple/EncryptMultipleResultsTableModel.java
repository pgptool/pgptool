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
package org.pgptool.gui.ui.encryptbackmultiple;

import java.util.List;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.pgptool.gui.app.Messages;

public class EncryptMultipleResultsTableModel implements TableModel {
  public static final int COLUMN_FILENAME = 0;
  public static final int COLUMN_RESULT = 1;
  public static final int COLUMN_DETAILS = 2;

  public static class Row {
    public final String filename;
    public final String result;
    public final String details;
    public final Throwable error;

    public Row(String filename, String result, String details, Throwable error) {
      this.filename = filename;
      this.result = result;
      this.details = details;
      this.error = error;
    }
  }

  private final List<Row> rows;

  public EncryptMultipleResultsTableModel(List<Row> rows) {
    this.rows = rows;
  }

  @Override
  public int getRowCount() {
    return rows.size();
  }

  @Override
  public int getColumnCount() {
    return 3;
  }

  @Override
  public String getColumnName(int columnIndex) {
    switch (columnIndex) {
      case COLUMN_FILENAME:
        return Messages.get("term.filename");
      case COLUMN_RESULT:
        return Messages.get("term.result");
      case COLUMN_DETAILS:
        return Messages.get("term.details");
      default:
        throw new IllegalArgumentException("Wrong column index: " + columnIndex);
    }
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return String.class;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Row r = rows.get(rowIndex);
    switch (columnIndex) {
      case -1:
        return r;
      case COLUMN_FILENAME:
        return " " + r.filename;
      case COLUMN_RESULT:
        return " " + r.result;
      case COLUMN_DETAILS:
        return " " + r.details;
      default:
        throw new IllegalArgumentException("Wrong column index: " + columnIndex);
    }
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    // immutable
  }

  @Override
  public void addTableModelListener(TableModelListener l) {}

  @Override
  public void removeTableModelListener(TableModelListener l) {}

  public List<Row> getRows() {
    return rows;
  }
}
