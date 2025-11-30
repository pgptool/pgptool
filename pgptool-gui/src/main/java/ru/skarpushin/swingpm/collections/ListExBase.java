package ru.skarpushin.swingpm.collections;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public abstract class ListExBase<E> implements ListEx<E> {
  protected int modCount = 1;
  protected final List<ListExEventListener<E>> listeners = new LinkedList<>();
  protected ListExEventListener<E> eventDispatcher;

  @Override
  public ListIterator<E> listIterator() {
    throw new IllegalStateException("Not implemented. Optional operation.");
  }

  @Override
  public ListIterator<E> listIterator(int index) {
    throw new IllegalStateException("Not implemented. Optional operation.");
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    if (c == null) {
      return false;
    }
    boolean ret = false;
    for (Object item : c) {
      ret |= remove(item);
    }
    return ret;
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new IllegalStateException("Not implemented. Optional operation.");
  }

  @Override
  public List<E> subList(int fromIndex, int toIndex) {
    throw new IllegalStateException("Not implemented. Optional operation.");
  }

  @Override
  public void addListExEventListener(ListExEventListener<E> l) {
    listeners.add(l);
  }

  @Override
  public void removeListExEventListener(ListExEventListener<E> l) {
    listeners.remove(l);
  }

  public final ListExEventListener<E> getEventDispatcher() {
    if (eventDispatcher == null) {
      eventDispatcher = buildEventDispatcher();
    }
    return eventDispatcher;
  }

  public final void setEventDispatcher(ListExEventListener<E> eventDispatcher) {
    this.eventDispatcher = eventDispatcher;
  }

  /**
   * Subclass might want to override and change strategy on how we dispatch events. For example to
   * make them as7ync or anything else
   *
   * @return event dispatcher
   */
  protected ListExEventListener<E> buildEventDispatcher() {
    return new ListExEventDispatcherDefaultImpl<>(this);
  }

  @Override
  public ListEx<E> getView(FilterPredicate<E> filterPredicate) {
    return new ListExViewImpl<>(this, filterPredicate);
  }

  @Override
  public void fireItemChanged(E item) {
    if (item == null) {
      getEventDispatcher().onItemChanged(null, -1);
    } else {
      getEventDispatcher().onItemChanged(item, indexOf(item));
    }
  }
}
