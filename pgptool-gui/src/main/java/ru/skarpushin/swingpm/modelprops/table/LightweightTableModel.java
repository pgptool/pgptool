package ru.skarpushin.swingpm.modelprops.table;

/**
 * Defines minimum set of methods required to render specific table.
 *
 * <p>Common methods, which dont depend on specific data schema is not part of this interface
 * because it could be easily implemented in generic way
 *
 * @author sergeyk
 */
public interface LightweightTableModel<E> {
  /**
   * Returns the number of columns in the model. A <code>JTable</code> uses this method to determine
   * how many columns it should create and display by default.
   *
   * @return the number of columns in the model
   */
  int getColumnCount();

  /**
   * Returns the name of the column at <code>columnIndex</code>. This is used to initialize the
   * table's column header name. Note: this name does not need to be unique; two columns in a table
   * can have the same name.
   *
   * @param columnIndex the index of the column
   * @return the name of the column
   */
  String getColumnName(int columnIndex);

  /**
   * Returns the most specific superclass for all the cell values in the column. This is used by the
   * <code>JTable</code> to set up a default renderer and editor for the column.
   *
   * @param columnIndex the index of the column
   * @return the common ancestor class of the object values in the model.
   */
  Class<?> getColumnClass(int columnIndex);

  /**
   * Returns the value for the cell at <code>columnIndex</code> and <code>rowIndex</code>.
   *
   * @param row data to get piece to render
   * @param columnIndex the column whose value is to be queried
   * @return the value Object at the specified cell
   */
  Object getValueAt(E row, int columnIndex);
}
