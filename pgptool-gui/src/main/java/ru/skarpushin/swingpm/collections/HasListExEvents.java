package ru.skarpushin.swingpm.collections;

public interface HasListExEvents<E> {

  void addListExEventListener(ListExEventListener<E> l);

  void removeListExEventListener(ListExEventListener<E> l);
}
