package ru.skarpushin.swingpm.tools.edt;

import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Impl which will detect abuse of synchronization on Edt thread that might cause deadlocks
 *
 * @author sergey.karpushin
 */
public class EdtInvokerFixedImpl implements EdtInvoker {
  private static final Logger log = LoggerFactory.getLogger(EdtInvokerFixedImpl.class);

  private volatile int invokers = 0;
  private final boolean halt = false;

  @Override
  public void invoke(Runnable task) {
    if (SwingUtilities.isEventDispatchThread()) {
      task.run();
      return;
    }

    try {
      invokers++;
      new BlockingInvocationOnEdtThread(task);
    } finally {
      invokers--;
    }
  }

  private class BlockingInvocationOnEdtThread implements Runnable {
    private static final long DEAD_LOCK_LONG = 5 * 1000;

    private final Runnable task;
    private volatile boolean executionCompleted = false;
    private Throwable exceptionHappened;

    public BlockingInvocationOnEdtThread(Runnable task) {
      this.task = task;

      SwingUtilities.invokeLater(this);

      long startedAt = System.currentTimeMillis();
      while (!executionCompleted) {
        Thread.yield();

        if (System.currentTimeMillis() - startedAt > DEAD_LOCK_LONG) {
          log.error(
              "Deadlock detected. Invokers: {}",
              invokers,
              new RuntimeException("Deadlock detected"));
          // Give time others to timeout and log stack traces
          safeSleep();
          // Now halt application
          System.exit(-2);
          return;
        }
      }

      if (exceptionHappened != null) {
        throw new RuntimeException("Unexpected exception happened", exceptionHappened);
      }
    }

    private void safeSleep() {
      try {
        Thread.sleep(DEAD_LOCK_LONG);
      } catch (InterruptedException ignored) {
        // do nothing
      }
    }

    @Override
    public void run() {
      try {
        // log.debug("Executing task: " + task);
        task.run();
      } catch (Throwable t) {
        exceptionHappened = t;
      } finally {
        // log.debug("Executing task done: " + task);
        executionCompleted = true;
      }
    }
  }
}
