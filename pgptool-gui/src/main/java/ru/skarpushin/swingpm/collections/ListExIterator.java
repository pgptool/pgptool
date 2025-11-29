package ru.skarpushin.swingpm.collections;

import com.google.common.base.Preconditions;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ListExIterator<E> implements Iterator<E> {
  private final ListExBase<E> list;
  private final boolean isReadonly;
  private int expectedModCount;
  private int idx = -1;
  private boolean removed = false;

  public ListExIterator(ListExBase<E> list, boolean isReadonly) {
    this.list = list;
    this.isReadonly = isReadonly;
    expectedModCount = list.modCount;
  }

  public ListExIterator(ListExBase<E> list) {
    this.list = list;
    this.isReadonly = false;
    expectedModCount = list.modCount;
  }

  @Override
  public boolean hasNext() {
    assertExpectedModCount();
    return idx + 1 < list.size();
  }

  @Override
  public E next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    removed = false;

    assertExpectedModCount();
    idx++;
    return list.get(idx);
  }

  private void assertExpectedModCount() {
    if (expectedModCount != list.modCount) {
      throw new ConcurrentModificationException();
    }
  }

  @Override
  public void remove() {
    assertExpectedModCount();
    Preconditions.checkState(idx >= 0);
    Preconditions.checkState(!removed);
    Preconditions.checkState(!isReadonly);

    removed = true;
    list.remove(idx);
    idx--;
    expectedModCount = list.modCount;
  }
}
