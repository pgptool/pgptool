package ru.skarpushin.swingpm.modelprops.virtualtable;

import org.summerb.utils.easycrud.api.dto.PagerParams;
import org.summerb.utils.easycrud.api.dto.PaginatedList;

public class BkgTaskLoadInitialData<E> extends BkgTask<E> implements Runnable {
  private PaginatedList<E> firstPage;

  public BkgTaskLoadInitialData(AsyncDataLoaderImpl<E> loader, Object statusId) {
    super(loader, statusId);
  }

  @Override
  public void perform() {
    // do load data
    firstPage = loader.virtualTableDataSource.loadData(new PagerParams(0, loader.pageSize));

    // do update table with it
    loader.edtInvoker.invoke(this);
  }

  @Override
  public void run() {
    if (stateId != loader.currentStateId) {
      return;
    }
    loader.modelVirtualTableProperty.setupWithInitialData(firstPage);
  }
}
