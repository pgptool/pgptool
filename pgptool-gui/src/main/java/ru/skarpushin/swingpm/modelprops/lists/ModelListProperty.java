package ru.skarpushin.swingpm.modelprops.lists;

import java.util.List;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExBase;
import ru.skarpushin.swingpm.collections.ListExEventDispatcherDefaultImpl;
import ru.skarpushin.swingpm.collections.ListExEventDispatcherEdtWrapperImpl;
import ru.skarpushin.swingpm.collections.ListExEventListener;
import ru.skarpushin.swingpm.collections.ListExImpl;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

/**
 * Model for the list property
 *
 * @author sergeyk
 * @param <E>
 */
public class ModelListProperty<E> extends ModelProperty<List<E>> implements ListExEventListener<E> {
  protected final ListExBase<E> list;
  protected EventListenerList listenerList = new EventListenerList();
  private ModelListPropertyAccessor<E> modelListPropertyAccessor;

  public ModelListProperty(Object source, ValueAdapter<List<E>> valueAdapter, String propertyName) {
    this(source, valueAdapter, propertyName, null);
  }

  public ModelListProperty(
      Object source,
      ValueAdapter<List<E>> valueAdapter,
      String propertyName,
      ListEx<ValidationError> veSource) {
    super(source, valueAdapter, propertyName, veSource);

    list = new ListExImpl<>(valueAdapter.getValue());
    list.addListExEventListener(this);
    if (isFireEventsInEventDispatchingThread()) {
      setupEventsOnEdt();
    }
  }

  protected void setupEventsOnEdt() {
    list.setEventDispatcher(
        new ListExEventDispatcherEdtWrapperImpl<>(new ListExEventDispatcherDefaultImpl<>(list)));
  }

  @Override
  public void setFireEventsInEventDispatchingThread(boolean fireEventsInEventDispatchingThread) {
    super.setFireEventsInEventDispatchingThread(fireEventsInEventDispatchingThread);

    if (fireEventsInEventDispatchingThread) {
      setupEventsOnEdt();
    } else {
      list.setEventDispatcher(new ListExEventDispatcherDefaultImpl<>(list));
    }
  }

  public ListEx<E> getList() {
    return list;
  }

  public ModelListPropertyAccessor<E> getModelListPropertyAccessor() {
    if (modelListPropertyAccessor == null) {
      modelListPropertyAccessor = new ListDefaultAccessorStrategy<>(this);
    }
    return modelListPropertyAccessor;
  }

  protected void doFireContentsChanged(int index0, int index1) {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ListDataListener.class) {
        if (e == null) {
          e = new ListDataEvent(source, ListDataEvent.CONTENTS_CHANGED, index0, index1);
        }
        ((ListDataListener) listeners[i + 1]).contentsChanged(e);
      }
    }
  }

  protected void doFireIntervalAdded(int index0, int index1) {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ListDataListener.class) {
        if (e == null) {
          e = new ListDataEvent(source, ListDataEvent.INTERVAL_ADDED, index0, index1);
        }
        ((ListDataListener) listeners[i + 1]).intervalAdded(e);
      }
    }
  }

  protected void doFireIntervalRemoved(int index0, int index1) {
    Object[] listeners = listenerList.getListenerList();
    ListDataEvent e = null;

    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == ListDataListener.class) {
        if (e == null) {
          e = new ListDataEvent(source, ListDataEvent.INTERVAL_REMOVED, index0, index1);
        }
        ((ListDataListener) listeners[i + 1]).intervalRemoved(e);
      }
    }
  }

  @Override
  public void onItemAdded(E item, int atIndex) {
    doFireIntervalAdded(atIndex, atIndex);
  }

  @Override
  public void onItemChanged(E item, int atIndex) {
    doFireContentsChanged(atIndex, atIndex);
  }

  @Override
  public void onItemRemoved(E item, int wasAtIndex) {
    doFireIntervalRemoved(wasAtIndex, wasAtIndex);
  }

  @Override
  public void onAllItemsRemoved(int sizeWas) {
    doFireIntervalRemoved(0, sizeWas - 1);
  }
}
