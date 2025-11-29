package ru.skarpushin.swingpm.collections;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Sub-class of {@link ListExImpl} impl which implements view capabilities. It allows to see list
 * but not change it. While this view is attached to parent it will propagate parents events.
 *
 * <p>Order of items in this view is not maintained and most likely will not be the same as order of
 * items in parent list
 *
 * @author sergey.karpushin
 */
public class ListExViewImpl<T> extends ListExImpl<T>
    implements ListExView<T>, ListExEventListener<T> {
  private static final String NOT_SUPPORTED_FOR_READONLY_LIST = "Not supported for readonly list";
  private final FilterPredicate<T> filterPredicate;
  private ListEx<T> parentList;

  public ListExViewImpl(ListEx<T> parentList, FilterPredicate<T> filterPredicate) {
    this(parentList, new ArrayList<>(), filterPredicate);
  }

  /**
   * Construct view
   *
   * @param parentList parent list
   * @param holderList list which will hold this view stateId. It's treated as simple List (not an
   *     ListEx subclass)
   * @param filterPredicate
   */
  public ListExViewImpl(
      ListEx<T> parentList, List<T> holderList, FilterPredicate<T> filterPredicate) {
    super(holderList);

    Preconditions.checkArgument(parentList != null);
    Preconditions.checkArgument(filterPredicate != null);

    this.parentList = parentList;
    this.filterPredicate = filterPredicate;

    collectAllApplicableItemsFromParent();

    parentList.addListExEventListener(this);
  }

  private void collectAllApplicableItemsFromParent() {
    for (T e : parentList) {
      if (!filterPredicate.isSuitable(e)) {
        continue;
      }

      this.list.add(e);
    }
  }

  @Override
  public boolean add(T e) {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public void add(int index, T element) {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public boolean addAll(Collection<? extends T> c) {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public boolean remove(Object o) {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public T remove(int index) {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public void clear() {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public T set(int index, T element) {
    throw new IllegalStateException(NOT_SUPPORTED_FOR_READONLY_LIST);
  }

  @Override
  public Iterator<T> iterator() {
    return new ListExIterator<>(this, true);
  }

  @Override
  public void detachView() {
    parentList.removeListExEventListener(this);
    parentList = null;
  }

  @Override
  public void onItemAdded(T item, int atIndex) {
    if (!filterPredicate.isSuitable(item)) {
      return;
    }
    internalAdd(item);
  }

  @Override
  public void onItemChanged(T item, int atIndex) {
    if (!filterPredicate.isSuitable(item)) {
      return;
    }
    getEventDispatcher().onItemChanged(item, indexOf(atIndex));
  }

  @Override
  public void onItemRemoved(T item, int wasAtIndex) {
    if (!filterPredicate.isSuitable(item)) {
      return;
    }
    internalRemove(item);
  }

  @Override
  public void onAllItemsRemoved(int sizeWas) {
    internalClear();
  }
}
