package ru.skarpushin.swingpm.modelprops.table;

import com.google.common.base.Preconditions;
import java.util.List;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExBase;
import ru.skarpushin.swingpm.collections.ListExEventDispatcherDefaultImpl;
import ru.skarpushin.swingpm.collections.ListExEventDispatcherEdtWrapperImpl;
import ru.skarpushin.swingpm.collections.ListExEventListener;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

/**
 * Table model. Own value is a selected row (1 row supported only by this impl)
 *
 * @author sergeyk
 * @param <E> contained data type
 */
public class ModelTableProperty<E> extends ModelProperty<E> implements ListExEventListener<E> {
  private final LightweightTableModel<E> lightweightTableModel;

  private final ListExBase<E> list;

  private final EventListenerList listenerList = new EventListenerList();

  private final ModelTablePropertyAccessor<E> modelTablePropertyAccessor =
      new ModelTablePropertyAccessor<>() {
        @Override
        public int getRowCount() {
          return getList().size();
        }

        @Override
        public int getColumnCount() {
          return lightweightTableModel.getColumnCount();
        }

        @Override
        public String getColumnName(int columnIndex) {
          return lightweightTableModel.getColumnName(columnIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
          return lightweightTableModel.getColumnClass(columnIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
          E row = list.get(rowIndex);
          return lightweightTableModel.getValueAt(row, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
          return lightweightTableModel instanceof LightweightTableModelEditable edit
              && edit.isCellEditable(rowIndex, columnIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
          if (lightweightTableModel instanceof LightweightTableModelEditable edit) {
            E row = list.get(rowIndex);
            edit.setValueAt(row, columnIndex, aValue);
            list.fireItemChanged(row);
          } else {
            throw new IllegalStateException("Operation setValueAt() not supported");
          }
        }

        @Override
        public void addTableModelListener(TableModelListener l) {
          listenerList.add(TableModelListener.class, l);
        }

        @Override
        public void removeTableModelListener(TableModelListener l) {
          listenerList.remove(TableModelListener.class, l);
        }

        @Override
        public String getPropertyName() {
          return ModelTableProperty.this.getPropertyName();
        }

        @Override
        public E findRowByIdx(int idx) {
          try {
            return getList().get(idx);
          } catch (IndexOutOfBoundsException e) {
            return null;
          }
        }

        @Override
        public int indexOf(E item) {
          return getList().indexOf(item);
        }

        @Override
        public ListEx<ValidationError> getValidationErrors() {
          return validationErrors;
        }
      };

  public ModelTableProperty(
      Object source,
      List<E> items,
      String propertyName,
      LightweightTableModel<E> lightweightTableModel) {
    this(source, items, propertyName, lightweightTableModel, null);
  }

  public ModelTableProperty(
      Object source,
      List<E> items,
      String propertyName,
      LightweightTableModel<E> lightweightTableModel,
      ListEx<ValidationError> veSource) {
    super(source, new ValueAdapterHolderImpl<>(), propertyName, veSource);

    Preconditions.checkArgument(lightweightTableModel != null);

    this.lightweightTableModel = lightweightTableModel;

    list = new ListExImpl<>(items);
    list.addListExEventListener(this);
    if (isFireEventsInEventDispatchingThread()) {
      setupEventsOnEdt();
    }
  }

  protected void setupEventsOnEdt() {
    list.setEventDispatcher(
        new ListExEventDispatcherEdtWrapperImpl<>(new ListExEventDispatcherDefaultImpl<>(list)));
  }

  @Override
  public void setFireEventsInEventDispatchingThread(boolean fireEventsInEventDispatchingThread) {
    super.setFireEventsInEventDispatchingThread(fireEventsInEventDispatchingThread);

    if (fireEventsInEventDispatchingThread) {
      setupEventsOnEdt();
    } else {
      list.setEventDispatcher(new ListExEventDispatcherDefaultImpl<>(list));
    }
  }

  public ListEx<E> getList() {
    return list;
  }

  public ModelTablePropertyAccessor<E> getModelTablePropertyAccessor() {
    return modelTablePropertyAccessor;
  }

  @Override
  public void onItemAdded(E item, int atIndex) {
    fireTableChanged(
        new TableModelEvent(
            modelTablePropertyAccessor,
            atIndex,
            atIndex,
            TableModelEvent.ALL_COLUMNS,
            TableModelEvent.INSERT));
  }

  @Override
  public void onItemChanged(E item, int atIndex) {
    fireTableChanged(
        new TableModelEvent(
            modelTablePropertyAccessor,
            atIndex,
            atIndex,
            TableModelEvent.ALL_COLUMNS,
            TableModelEvent.UPDATE));
  }

  @Override
  public void onItemRemoved(E item, int wasAtIndex) {
    fireTableChanged(
        new TableModelEvent(
            modelTablePropertyAccessor,
            wasAtIndex,
            wasAtIndex,
            TableModelEvent.ALL_COLUMNS,
            TableModelEvent.DELETE));
  }

  @Override
  public void onAllItemsRemoved(int sizeWas) {
    fireTableChanged(
        new TableModelEvent(
            modelTablePropertyAccessor,
            0,
            sizeWas - 1,
            TableModelEvent.ALL_COLUMNS,
            TableModelEvent.DELETE));
  }

  /**
   * Forwards the given notification event to all <code>TableModelListeners</code> that registered
   * themselves as listeners for this table model.
   *
   * @param e the event to be forwarded
   * @see TableModelEvent
   * @see EventListenerList
   */
  public void fireTableChanged(TableModelEvent e) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first, notifying
    // those that are interested in this event
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == TableModelListener.class) {
        ((TableModelListener) listeners[i + 1]).tableChanged(e);
      }
    }
  }
}
