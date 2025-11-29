package ru.skarpushin.swingpm.collections;

import java.util.EventListener;

public interface ListExEventListener<E> extends EventListener {
  void onItemAdded(E item, int atIndex);

  void onItemChanged(E item, int atIndex);

  void onItemRemoved(E item, int wasAtIndex);

  void onAllItemsRemoved(int sizeWas);
}
