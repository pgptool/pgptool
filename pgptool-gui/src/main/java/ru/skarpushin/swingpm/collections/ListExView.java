package ru.skarpushin.swingpm.collections;

public interface ListExView<E> extends ListEx<E> {
  /**
   * Stop synchronization with parent list. Getters will still work as soon as view holds copy of
   * parent list
   */
  void detachView();
}
