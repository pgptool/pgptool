package ru.skarpushin.swingpm.modelprops.virtualtable;

import org.summerb.utils.easycrud.api.dto.PagerParams;
import org.summerb.utils.easycrud.api.dto.PaginatedList;

public class BkgTaskLoadNewPage<E> extends BkgTask<E> implements Runnable, HasPageIdx {

  protected int pageIdx;
  private PaginatedList<E> page;

  public BkgTaskLoadNewPage(AsyncDataLoaderImpl<E> loader, Object statusId, int pageIdx) {
    super(loader, statusId);
    this.pageIdx = pageIdx;
  }

  @Override
  public void perform() {
    page =
        loader.virtualTableDataSource.loadData(
            new PagerParams(pageIdx * loader.pageSize, loader.pageSize));
    loader.edtInvoker.invoke(this);
  }

  @Override
  public void run() {
    if (stateId != loader.currentStateId) {
      return;
    }
    loader.modelVirtualTableProperty.handleNewDataLoaded(page);
  }

  @Override
  public int getPageIdx() {
    return pageIdx;
  }

  @Override
  public String toString() {
    return "BkgTaskLoadNewPage [pageIdx=" + pageIdx + "]";
  }
}
