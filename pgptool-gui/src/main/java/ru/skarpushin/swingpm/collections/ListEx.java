package ru.skarpushin.swingpm.collections;

import java.util.List;

public interface ListEx<E> extends List<E>, HasListExEvents<E> {
  void fireItemChanged(E item);

  /**
   * Returning read-only view of this list which will containing only those items which considered
   * as suitable by filterPredicate
   *
   * @param filterPredicate predicate which decide if list item is suitable for new view or not
   * @return readonly view of underlying list
   */
  ListEx<E> getView(FilterPredicate<E> filterPredicate);
}
