package ru.skarpushin.swingpm.collections;

import com.google.common.base.Preconditions;

/**
 * Default impl which will just notify all listeners
 *
 * @author sergey.karpushin
 * @param <E>
 */
public class ListExEventDispatcherDefaultImpl<E> implements ListExEventListener<E> {
  private final ListExBase<E> list;

  public ListExEventDispatcherDefaultImpl(ListExBase<E> list) {
    Preconditions.checkArgument(list != null);
    this.list = list;
  }

  @Override
  public void onItemAdded(E item, int atIndex) {
    for (ListExEventListener<E> listener : list.listeners) {
      listener.onItemAdded(item, atIndex);
    }
  }

  @Override
  public void onItemChanged(E item, int atIndex) {
    for (ListExEventListener<E> listener : list.listeners) {
      listener.onItemChanged(item, atIndex);
    }
  }

  @Override
  public void onItemRemoved(E item, int wasAtIndex) {
    for (ListExEventListener<E> listener : list.listeners) {
      listener.onItemRemoved(item, wasAtIndex);
    }
  }

  @Override
  public void onAllItemsRemoved(int sizeWas) {
    for (ListExEventListener<E> listener : list.listeners) {
      listener.onAllItemsRemoved(sizeWas);
    }
  }

  protected boolean isListenerCompliant(Object[] listeners, int i) {
    return listeners[i] == ListExEventListener.class;
  }
}
