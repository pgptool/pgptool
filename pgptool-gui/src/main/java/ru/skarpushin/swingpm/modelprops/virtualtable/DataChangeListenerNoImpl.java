package ru.skarpushin.swingpm.modelprops.virtualtable;

/**
 * Responsible for handling situations when cached data must be invalidated or just updated (for
 * single row change).
 *
 * <p>WARNING: this impl doesn't actually track changes - it's just provide method {@link
 * #triggerRowChanged(Object)} and {@link #triggerRowCountChanged()} so someone external (or
 * subclass) will call it
 */
public class DataChangeListenerNoImpl<E> {
  protected AsyncDataLoader<E> asyncDataLoader;

  public DataChangeListenerNoImpl(AsyncDataLoader<E> asyncDataLoader) {
    this.asyncDataLoader = asyncDataLoader;
  }

  /**
   * Impl must call it when one row change is detected.
   *
   * @param row new version of row which is eligible for display and was changed)
   */
  public void triggerRowChanged(E row) {
    if (!asyncDataLoader.getVirtualTableDataSource().isSuitable(row)) {
      // NOTE: What if it was suitable but it's not after change?...
      int idx =
          asyncDataLoader
              .getModelVirtualTableProperty()
              .getModelTablePropertyAccessor()
              .indexOf(row);
      if (idx >= 0) {
        triggerRowCountChanged();
      }
      return;
    }
    asyncDataLoader.handleRowChanged(row);
  }

  public void triggerRowCountChanged() {
    asyncDataLoader.handleRowCountChanged();
  }

  public AsyncDataLoader<E> getAsyncDataLoader() {
    return asyncDataLoader;
  }

  public void setAsyncDataLoader(AsyncDataLoader<E> asyncDataLoader) {
    this.asyncDataLoader = asyncDataLoader;
  }
}
