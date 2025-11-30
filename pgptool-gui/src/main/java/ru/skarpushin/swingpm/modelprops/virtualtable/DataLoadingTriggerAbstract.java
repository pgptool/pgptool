package ru.skarpushin.swingpm.modelprops.virtualtable;

import java.util.Comparator;
import java.util.List;

/**
 * Responsible for tracking scroll and deciding whenever additional data needs to be loaded to be
 * displayed in the table
 */
public abstract class DataLoadingTriggerAbstract<E> {
  protected AsyncDataLoader<E> asyncDataLoader;

  protected DataLoadingTriggerAbstract(AsyncDataLoader<E> asyncDataLoader) {
    this.asyncDataLoader = asyncDataLoader;
    asyncDataLoader.setDataLoadingTriggerAbstract(this);
  }

  /** Impl must call it whenever new range of data displayed */
  protected void triggerEnsureDataLoaded(int pageIdx) {
    asyncDataLoader.handlePageNeedsToBeLoaded(pageIdx);
  }

  public abstract int getHighPriorityRow();

  public AsyncDataLoader<E> getAsyncDataLoader() {
    return asyncDataLoader;
  }

  public void setAsyncDataLoader(AsyncDataLoader<E> asyncDataLoader) {
    this.asyncDataLoader = asyncDataLoader;
  }

  public void sort(List<HasPageIdx> loadPageTasks) {
    loadPageTasks.sort(new PagesComparator(getHighPriorityRow()));
  }

  private static class PagesComparator implements Comparator<HasPageIdx> {
    private final int highPriorityRow;

    public PagesComparator(int highPriorityRow) {
      this.highPriorityRow = highPriorityRow;
    }

    @Override
    public int compare(HasPageIdx o1, HasPageIdx o2) {
      return Math.abs(o1.getPageIdx() - highPriorityRow)
          - Math.abs(o2.getPageIdx() - highPriorityRow);
    }
  }
}
