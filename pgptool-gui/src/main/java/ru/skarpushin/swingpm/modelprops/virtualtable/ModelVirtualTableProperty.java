package ru.skarpushin.swingpm.modelprops.virtualtable;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summerb.utils.easycrud.api.dto.PaginatedList;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.LightweightTableModel;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

/**
 * Model contain not a sequential list of records, but a collection of pages with possible gaps.
 * i.e. it can contain data for rows 0-99,1000-1099, but data for 100-999 might be not loaded into
 * this model. This data might be loaded later as user scroll table.
 *
 * @param <E> type of data displayed in table
 * @see DataChangeListenerNoImpl
 * @see DataLoadingTriggerAbstract
 * @see AsyncDataLoader
 * @see VirtualTableDataSource
 */
public class ModelVirtualTableProperty<E> extends ModelProperty<E> {
  private static final Logger log = LoggerFactory.getLogger(ModelVirtualTableProperty.class);
  protected final long pageSize;
  private final LightweightTableModel<E> lightweightTableModel;
  protected List<PaginatedList<E>> loadedData = new ArrayList<>();
  private RowRetrieverFeedbackHandler rowNotFoundFeedbackHandler;
  private final EventListenerList listenerList = new EventListenerList();
  private int lastRowIdxRequested = -1;
  private E lastRowRequested;
  private final RowEqualityChecker<E> rowEqualityChecker;
  private final ModelProperty<Boolean> hasData;
  private final ModelVirtualTablePropertyAccessor<E> modelTablePropertyAccessor =
      new ModelVirtualTablePropertyAccessor<>() {
        @Override
        public int getRowCount() {
          List<PaginatedList<E>> data = loadedData;
          if (data.isEmpty()) {
            return 0;
          }
          return (int) data.get(0).getTotalResults();
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
        public boolean isCellEditable(int rowIndex, int columnIndex) {
          return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
          E row = getCachedOrFindRowByIdx(rowIndex);
          if (columnIndex == -1) {
            return row;
          }
          return lightweightTableModel.getValueAt(row, columnIndex);
        }

        private E getCachedOrFindRowByIdx(int rowIndex) {
          // Cache row object, since table will request columns sequentially
          // for same row several times - avoid traversing all pages
          E row = null;
          if (rowIndex == lastRowIdxRequested) {
            row = lastRowRequested;
          } else {
            // log.debug(String.format("getValueAt %d, %d", rowIndex,
            // columnIndex));
            row = findRowByIdx(rowIndex);
            lastRowIdxRequested = rowIndex;
            lastRowRequested = row;

            if (rowNotFoundFeedbackHandler != null) {
              rowNotFoundFeedbackHandler.handleRowRequested(rowIndex, row != null);
            }
          }
          return row;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
          throw new IllegalStateException("Operation is not supported");
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
          return ModelVirtualTableProperty.this.getPropertyName();
        }

        @Override
        public E findRowByIdx(int idx) {
          List<PaginatedList<E>> data = loadedData;
          for (PaginatedList<E> page : data) {
            if (page.getPagerParams().getOffset() <= idx
                && idx < page.getPagerParams().getOffset() + page.getPagerParams().getMax()) {
              try {
                return page.getItems().get(idx - (int) page.getPagerParams().getOffset());
              } catch (IndexOutOfBoundsException ioobe) {
                return null;
              }
            }
          }
          return null;
        }

        @Override
        public int indexOf(E subject) {
          List<PaginatedList<E>> data = loadedData;
          for (PaginatedList<E> page : data) {
            for (int i = 0; i < page.getItems().size(); i++) {
              E cur = page.getItems().get(i);
              if (rowEqualityChecker.areEquals(cur, subject)) {
                int idx = (int) page.getPagerParams().getOffset() + i;
                return idx;
              }
            }
          }
          return -1;
        }

        @Override
        public ListEx<ValidationError> getValidationErrors() {
          return validationErrors;
        }

        @Override
        public ModelPropertyAccessor<Boolean> getHasData() {
          return hasData.getModelPropertyAccessor();
        }
      };
  private final ValueAdapter<Boolean> hasDataAdapter =
      new ValueAdapter<>() {
        @Override
        public Boolean getValue() {
          return modelTablePropertyAccessor.getRowCount() > 0;
        }

        @Override
        public void setValue(Boolean value) {
          // do nothing
        }
      };

  public ModelVirtualTableProperty(
      Object source,
      long pageSize,
      String propertyName,
      LightweightTableModel<E> lightweightTableModel,
      RowEqualityChecker<E> rowEqualityChecker) {
    this(source, pageSize, propertyName, lightweightTableModel, rowEqualityChecker, null);
  }

  public ModelVirtualTableProperty(
      Object source,
      long pageSize,
      String propertyName,
      LightweightTableModel<E> lightweightTableModel,
      RowEqualityChecker<E> rowEqualityChecker,
      ListEx<ValidationError> veSource) {
    super(source, new ValueAdapterHolderImpl<>(), propertyName, veSource);

    this.lightweightTableModel = lightweightTableModel;
    this.pageSize = pageSize;
    this.rowEqualityChecker = rowEqualityChecker;

    hasData = new ModelProperty<>(this, hasDataAdapter, "hasData");
  }

  public ModelVirtualTablePropertyAccessor<E> getModelTablePropertyAccessor() {
    return modelTablePropertyAccessor;
  }

  protected void setupWithInitialData(PaginatedList<E> firstPage) {
    log.debug("setupWithInitialData(): {}", firstPage);
    if (firstPage.getTotalResults() == 0) {
      return;
    }
    loadedData.add(firstPage);
    fireTableChanged(
        new TableModelEvent(
            modelTablePropertyAccessor,
            0,
            (int) (firstPage.getTotalResults() - 1),
            TableModelEvent.ALL_COLUMNS,
            TableModelEvent.INSERT));
  }

  @SuppressWarnings("unchecked")
  protected void replaceCurrentDataWith(
      PaginatedList<E> firstPage, PaginatedList<E> optionalCurrentPage) {
    log.debug("replaceCurrentDataWith(): {}, {}", firstPage, optionalCurrentPage);

    int currentSize = !loadedData.isEmpty() ? (int) loadedData.get(0).getTotalResults() : 0;
    int newSize = (int) firstPage.getTotalResults();

    loadedData.clear();
    loadedData.add(firstPage);
    if (optionalCurrentPage != null) {
      loadedData.add(optionalCurrentPage);
    }

    if (newSize > currentSize) {
      fireTableChanged(
          new TableModelEvent(
              modelTablePropertyAccessor,
              0,
              newSize - currentSize,
              TableModelEvent.ALL_COLUMNS,
              TableModelEvent.INSERT));
    } else if (newSize < currentSize) {
      fireTableChanged(
          new TableModelEvent(
              modelTablePropertyAccessor,
              0,
              currentSize - newSize,
              TableModelEvent.ALL_COLUMNS,
              TableModelEvent.DELETE));
    }

    if (newSize > 0) {
      fireTableChanged(
          new TableModelEvent(
              modelTablePropertyAccessor,
              0,
              newSize - 1,
              TableModelEvent.ALL_COLUMNS,
              TableModelEvent.UPDATE));
    }

    hasData.firePropertyChanged(currentSize > 0, newSize > 0);

    // Update selected row value
    if (getValue() != null) {
      int idx = modelTablePropertyAccessor.indexOf(getValue());
      if (idx < 0) {
        setValueByOwner(null);
      } else {
        setValueByOwner((E) modelTablePropertyAccessor.getValueAt(idx, -1));
      }
    }
  }

  protected void handleNewDataLoaded(PaginatedList<E> page) {
    log.debug("handleNewDataLoaded(): {}", page);

    Preconditions.checkState(page.getPagerParams().getMax() == pageSize);

    loadedData.add(page);
    int idxStart = (int) (page.getPagerParams().getOffset());
    int idxEnd = Math.min((int) page.getTotalResults() - 1, (int) (idxStart + pageSize - 1));
    fireTableChanged(
        new TableModelEvent(
            modelTablePropertyAccessor,
            idxStart,
            idxEnd,
            TableModelEvent.ALL_COLUMNS,
            TableModelEvent.UPDATE));
  }

  protected void handleRowChanged(E rowChanged) {
    log.debug("handleRowChanged(): {}", rowChanged);

    if (getValue() != null && rowEqualityChecker.areEquals(getValue(), rowChanged)) {
      setValueByOwner(rowChanged);
    }

    List<PaginatedList<E>> data = loadedData;
    for (PaginatedList<E> page : data) {
      for (int i = 0; i < page.getItems().size(); i++) {
        E cur = page.getItems().get(i);
        if (!rowEqualityChecker.areEquals(cur, rowChanged)) {
          continue;
        }
        int idx = (int) page.getPagerParams().getOffset() + i;
        page.getItems().set(i, rowChanged);
        fireTableChanged(
            new TableModelEvent(
                modelTablePropertyAccessor,
                idx,
                idx,
                TableModelEvent.ALL_COLUMNS,
                TableModelEvent.UPDATE));
        return;
      }
    }
    log.warn("Received handleRowChanged() but row not found: {}", rowChanged);
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
    lastRowIdxRequested = -1;

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

  public RowRetrieverFeedbackHandler getRowNotFoundFeedbackHandler() {
    return rowNotFoundFeedbackHandler;
  }

  public void setRowNotFoundFeedbackHandler(RowRetrieverFeedbackHandler rowNotFoundFeedback) {
    this.rowNotFoundFeedbackHandler = rowNotFoundFeedback;
  }
}
