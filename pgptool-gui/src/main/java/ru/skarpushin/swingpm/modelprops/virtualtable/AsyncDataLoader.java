package ru.skarpushin.swingpm.modelprops.virtualtable;

import ru.skarpushin.swingpm.tools.edt.Edt;
import ru.skarpushin.swingpm.tools.edt.EdtInvoker;

/**
 * Responsible for async data loading and updating model with it (have to be in sync with what
 * {@link DataChangeListenerNoImpl} does)
 */
public abstract class AsyncDataLoader<E> {
  protected final int pageSize;
  protected ModelVirtualTableProperty<E> modelVirtualTableProperty;
  protected VirtualTableDataSource<E> virtualTableDataSource;
  protected EdtInvoker edtInvoker = Edt.getEdtInvoker();
  protected DataLoadingTriggerAbstract<E> dataLoadingTriggerAbstract;

  protected AsyncDataLoader(
      ModelVirtualTableProperty<E> modelVirtualTableProperty,
      VirtualTableDataSource<E> virtualTableDataSource,
      int pageSize) {
    this.modelVirtualTableProperty = modelVirtualTableProperty;
    this.virtualTableDataSource = virtualTableDataSource;
    this.pageSize = pageSize;
  }

  /** Impl must rar-down any background things it might have */
  public void tearDown() {}

  /** IMPORTANT: Assuming this will be called on EDT */
  protected abstract void handleRowCountChanged();

  /** IMPORTANT: Assuming this will be called on EDT */
  protected abstract void handleRowChanged(E row);

  /** For IMPL: might be called several times for same page */
  protected abstract void handlePageNeedsToBeLoaded(int pageIdx);

  public ModelVirtualTableProperty<E> getModelVirtualTableProperty() {
    return modelVirtualTableProperty;
  }

  public void setModelVirtualTableProperty(ModelVirtualTableProperty<E> modelVirtualTableProperty) {
    this.modelVirtualTableProperty = modelVirtualTableProperty;
  }

  public VirtualTableDataSource<E> getVirtualTableDataSource() {
    return virtualTableDataSource;
  }

  public void setVirtualTableDataSource(VirtualTableDataSource<E> virtualTableDataSource) {
    this.virtualTableDataSource = virtualTableDataSource;
  }

  public DataLoadingTriggerAbstract<E> getDataLoadingTriggerAbstract() {
    return dataLoadingTriggerAbstract;
  }

  public void setDataLoadingTriggerAbstract(
      DataLoadingTriggerAbstract<E> dataLoadingTriggerAbstract) {
    this.dataLoadingTriggerAbstract = dataLoadingTriggerAbstract;
  }
}
