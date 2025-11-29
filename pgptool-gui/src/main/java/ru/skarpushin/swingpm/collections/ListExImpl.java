package ru.skarpushin.swingpm.collections;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ListExImpl<E> extends ListExBase<E> {
  protected final List<E> list;

  public ListExImpl(List<E> list) {
    Preconditions.checkArgument(list != null);
    // Preconditions.checkArgument(!(list instanceof ListEx),
    // "It's not supported to base ListExImpl on other ListEx, creaate view
    // instead");

    this.list = list;
  }

  public ListExImpl() {
    this(new ArrayList<>());
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return list.contains(o);
  }

  @Override
  public boolean add(E e) {
    return internalAdd(e);
  }

  protected boolean internalAdd(E e) {
    modCount++;
    int newIndex = size();
    boolean changed = list.add(e);
    if (changed) {
      getEventDispatcher().onItemAdded(e, newIndex);
    }
    return changed;
  }

  @Override
  public void add(int index, E e) {
    internalAdd(index, e);
  }

  protected void internalAdd(int index, E e) {
    modCount++;
    list.add(index, e);
    getEventDispatcher().onItemAdded(e, index);
  }

  @Override
  public boolean addAll(Collection<? extends E> c) {
    return internalAddAll(c);
  }

  protected boolean internalAddAll(Collection<? extends E> c) {
    boolean changed = false;
    for (E e : c) {
      changed |= add(e);
    }
    return changed;
  }

  @Override
  public boolean addAll(int index, Collection<? extends E> c) {
    return internalAddAll(index, c);
  }

  protected boolean internalAddAll(int index, Collection<? extends E> c) {
    int sizeBefore = size();
    int insertionIndex = index;
    for (E e : c) {
      add(insertionIndex, e);
      insertionIndex++;
    }
    return sizeBefore != size();
  }

  @Override
  public boolean remove(Object o) {
    return internalRemove(o);
  }

  protected boolean internalRemove(Object o) {
    int index = indexOf(o);
    boolean contained = index != -1;
    if (contained) {
      internalRemove(index);
    }
    return contained;
  }

  @Override
  public E remove(int index) {
    return internalRemove(index);
  }

  protected E internalRemove(int index) {
    modCount++;
    E removedElement = list.remove(index);
    getEventDispatcher().onItemRemoved(removedElement, index);
    return removedElement;
  }

  @Override
  public void clear() {
    internalClear();
  }

  protected void internalClear() {
    if (isEmpty()) {
      return;
    }

    modCount++;
    int sizeWas = size();
    list.clear();
    getEventDispatcher().onAllItemsRemoved(sizeWas);
  }

  @Override
  public E get(int index) {
    return list.get(index);
  }

  @Override
  public E set(int index, E element) {
    return internalSet(index, element);
  }

  protected E internalSet(int index, E element) {
    E previousElement = list.set(index, element);
    getEventDispatcher().onItemChanged(element, index);
    return previousElement;
  }

  @Override
  public int indexOf(Object o) {
    return list.indexOf(o);
  }

  @Override
  public Iterator<E> iterator() {
    return new ListExIterator<>(this);
  }

  @Override
  public Object[] toArray() {
    return list.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return list.toArray(a);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return list.containsAll(c);
  }

  @Override
  public int lastIndexOf(Object o) {
    return list.lastIndexOf(o);
  }
}
