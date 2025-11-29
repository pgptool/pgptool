package ru.skarpushin.swingpm.modelprops.table;

public interface LightweightTableModelEditable<E> {
  boolean isCellEditable(int rowIndex, int columnIndex);

  void setValueAt(E row, int columnIndex, Object aValue);
}
