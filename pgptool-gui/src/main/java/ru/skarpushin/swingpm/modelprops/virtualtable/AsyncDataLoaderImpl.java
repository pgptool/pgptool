package ru.skarpushin.swingpm.modelprops.virtualtable;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summerb.utils.threads.RecurringBackgroundTask;

public class AsyncDataLoaderImpl<E> extends AsyncDataLoader<E> implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(AsyncDataLoaderImpl.class);

  protected int lastPageRequested = -1;
  protected volatile Object currentStateId;
  protected volatile BkgTask<E> queue;

  private long delayMs = 100;

  private final RecurringBackgroundTask bkgTask;

  public AsyncDataLoaderImpl(
      ModelVirtualTableProperty<E> modelVirtualTableProperty,
      VirtualTableDataSource<E> virtualTableDataSource,
      int pageSize) {
    super(modelVirtualTableProperty, virtualTableDataSource, pageSize);

    resetState();
    queue = new BkgTaskLoadInitialData<>(this, currentStateId);

    bkgTask = new RecurringBackgroundTask(this, delayMs);
  }

  @Override
  public void tearDown() {
    super.tearDown();
    resetState();
    bkgTask.tearDown(2000);
  }

  protected void resetState() {
    currentStateId = new Object();
    // lastPageRequested = -1;
  }

  @Override
  protected void handleRowCountChanged() {
    resetState();

    queue = new BkgTaskInvalidateCache<>(this, currentStateId);
  }

  @Override
  protected void handlePageNeedsToBeLoaded(int pageIdx) {
    if (pageIdx == lastPageRequested) {
      return;
    }
    lastPageRequested = pageIdx;

    if (isPageAlreadyHandled(pageIdx)) {
      return;
    }

    log.debug("Page load scheduled: {}", pageIdx);
    BkgTask<E> tail = queue.findTail();
    tail.next = new BkgTaskLoadNewPage<>(this, currentStateId, pageIdx);
  }

  /** Is it scheduled or even received response? */
  private boolean isPageAlreadyHandled(int pageIdx) {
    if (pageIdx == 0) {
      return true;
    }

    BkgTask<E> cur = queue;
    while (cur != null) {
      if (cur.stateId != currentStateId) {
        // NOTE: Yes, reference equality used here. Yes, by intention
        // (for faster comparison).
        cur = cur.next;
        continue;
      }
      if (!(cur instanceof BkgTaskLoadNewPage)) {
        cur = cur.next;
        continue;
      }
      if (pageIdx == ((BkgTaskLoadNewPage<E>) cur).pageIdx) {
        return true;
      }

      cur = cur.next;
    }
    return false;
  }

  @Override
  public void run() {
    try {
      BkgTask<E> task = queue.findNextForProcessing();
      if (task == null) {
        return;
      }

      if (dataLoadingTriggerAbstract != null && task instanceof BkgTaskLoadNewPage) {
        task = findHigherPriorityPageToLoad(task);
        if (task == null) {
          return;
        }
      }

      log.debug("Picked task: {}", task);
      task.perform();
      task.status = BkgTask.STATUS_PERFORMED;
    } catch (Throwable e) {
      log.error("Bkg iteration failed", e);
    }
  }

  @SuppressWarnings("unchecked")
  private BkgTask<E> findHigherPriorityPageToLoad(BkgTask<E> task) {
    List<HasPageIdx> loadPageTasks = new ArrayList<>();
    BkgTask<E> cur = task;
    while (cur != null) {
      // sanity check - if state changed - halt
      if (cur.stateId != currentStateId) {
        return null;
      }
      if (cur.status == BkgTask.STATUS_PERFORMED) {
        cur = cur.next;
        continue;
      }
      if (!(cur instanceof BkgTaskLoadNewPage)) {
        cur = cur.next;
        continue;
      }
      loadPageTasks.add((BkgTaskLoadNewPage<E>) cur);
      cur = cur.next;
    }
    if (dataLoadingTriggerAbstract != null) {
      dataLoadingTriggerAbstract.sort(loadPageTasks);
    }
    task = (BkgTaskLoadNewPage<E>) loadPageTasks.get(0);
    return task;
  }

  @Override
  protected void handleRowChanged(E row) {
    modelVirtualTableProperty.handleRowChanged(row);
  }

  public long getDelayMs() {
    return delayMs;
  }

  public void setDelayMs(long delay) {
    this.delayMs = delay;
  }
}
