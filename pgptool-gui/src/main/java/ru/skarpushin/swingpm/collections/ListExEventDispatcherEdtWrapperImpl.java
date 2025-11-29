package ru.skarpushin.swingpm.collections;

import javax.swing.SwingUtilities;
import ru.skarpushin.swingpm.tools.edt.Edt;

public class ListExEventDispatcherEdtWrapperImpl<E> implements ListExEventListener<E> {
  private final ListExEventListener<E> underlyningDispatcher;

  public ListExEventDispatcherEdtWrapperImpl(ListExEventListener<E> underlyningDispatcher) {
    this.underlyningDispatcher = underlyningDispatcher;
  }

  @Override
  public void onItemAdded(final E item, final int atIndex) {
    if (SwingUtilities.isEventDispatchThread()) {
      underlyningDispatcher.onItemAdded(item, atIndex);
    } else {
      try {
        Edt.invokeOnEdtAndWait(() -> underlyningDispatcher.onItemAdded(item, atIndex));
      } catch (Throwable e) {
        throw new RuntimeException("Faield to invoke handler on Edt thread", e);
      }
    }
  }

  @Override
  public void onItemChanged(final E item, final int atIndex) {
    if (SwingUtilities.isEventDispatchThread()) {
      underlyningDispatcher.onItemChanged(item, atIndex);
    } else {
      try {
        Edt.invokeOnEdtAndWait(() -> underlyningDispatcher.onItemChanged(item, atIndex));
      } catch (Throwable e) {
        throw new RuntimeException("Faield to invoke handler on Edt thread", e);
      }
    }
  }

  @Override
  public void onItemRemoved(final E item, final int wasAtIndex) {
    if (SwingUtilities.isEventDispatchThread()) {
      underlyningDispatcher.onItemRemoved(item, wasAtIndex);
    } else {
      try {
        Edt.invokeOnEdtAndWait(() -> underlyningDispatcher.onItemRemoved(item, wasAtIndex));
      } catch (Throwable e) {
        throw new RuntimeException("Faield to invoke handler on Edt thread", e);
      }
    }
  }

  @Override
  public void onAllItemsRemoved(final int sizeWas) {
    if (SwingUtilities.isEventDispatchThread()) {
      underlyningDispatcher.onAllItemsRemoved(sizeWas);
    } else {
      try {
        Edt.invokeOnEdtAndWait(() -> underlyningDispatcher.onAllItemsRemoved(sizeWas));
      } catch (Throwable e) {
        throw new RuntimeException("Faield to invoke handler on Edt thread", e);
      }
    }
  }
}
