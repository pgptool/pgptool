package ru.skarpushin.swingpm.modelprops.virtualtable;

import org.summerb.utils.easycrud.api.dto.PagerParams;
import org.summerb.utils.easycrud.api.dto.PaginatedList;

public class BkgTaskInvalidateCache<E> extends BkgTask<E> implements Runnable {
  protected int pageIdx;
  private PaginatedList<E> firstPage;
  private PaginatedList<E> currentPage;

  public BkgTaskInvalidateCache(AsyncDataLoaderImpl<E> loader, Object statusId) {
    super(loader, statusId);
  }

  @Override
  public void perform() {
    // do load data
    firstPage = loader.virtualTableDataSource.loadData(new PagerParams(0, loader.pageSize));
    currentPage = null;
    if (loader.lastPageRequested > 0
        && firstPage.getTotalResults() > (long) loader.lastPageRequested * loader.pageSize) {
      currentPage =
          loader.virtualTableDataSource.loadData(
              new PagerParams((long) loader.lastPageRequested * loader.pageSize, loader.pageSize));
    } else {
      loader.lastPageRequested = -1;
    }

    // do update table with it
    loader.edtInvoker.invoke(this);
  }

  @Override
  public void run() {
    if (stateId != loader.currentStateId) {
      return;
    }
    loader.modelVirtualTableProperty.replaceCurrentDataWith(firstPage, currentPage);
  }
}
