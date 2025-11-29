package ru.skarpushin.swingpm.modelprops.lists;

import javax.swing.event.ListDataListener;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;

public class ListDefaultAccessorStrategy<E> implements ModelListPropertyAccessor<E> {
  private final ModelListProperty<E> property;

  public ListDefaultAccessorStrategy(ModelListProperty<E> property) {
    this.property = property;
  }

  @Override
  public int getSize() {
    return property.list.size();
  }

  @Override
  public E getElementAt(int index) {
    return property.list.get(index);
  }

  @Override
  public E get(int index) {
    return property.list.get(index);
  }

  @Override
  public int indexOf(E item) {
    return property.list.indexOf(item);
  }

  @Override
  public void addListDataListener(ListDataListener l) {
    property.listenerList.add(ListDataListener.class, l);
  }

  @Override
  public void removeListDataListener(ListDataListener l) {
    property.listenerList.remove(ListDataListener.class, l);
  }

  @Override
  public String getPropertyName() {
    return property.getPropertyName();
  }

  @Override
  public ListEx<ValidationError> getValidationErrors() {
    return property.getValidationErrors();
  }
}
