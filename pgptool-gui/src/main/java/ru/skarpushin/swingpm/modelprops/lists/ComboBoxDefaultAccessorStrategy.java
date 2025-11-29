package ru.skarpushin.swingpm.modelprops.lists;

import javax.swing.event.ListDataListener;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;

public class ComboBoxDefaultAccessorStrategy<E> implements ModelSelInComboBoxPropertyAccessor<E> {
  private final ModelSelInComboBoxProperty<E> property;

  public ComboBoxDefaultAccessorStrategy(ModelSelInComboBoxProperty<E> property) {
    this.property = property;
  }

  @Override
  public E get(int idx) {
    return property.optionsAccessor.get(idx);
  }

  @Override
  public int indexOf(E item) {
    return property.optionsAccessor.indexOf(item);
  }

  @Override
  public int getSize() {
    return property.optionsAccessor.getSize();
  }

  @Override
  public E getElementAt(int index) {
    return property.optionsAccessor.getElementAt(index);
  }

  @Override
  public void addListDataListener(ListDataListener l) {
    property.optionsAccessor.addListDataListener(l);
  }

  @Override
  public void removeListDataListener(ListDataListener l) {
    property.optionsAccessor.removeListDataListener(l);
  }

  @Override
  public String getPropertyName() {
    return property.getPropertyName();
  }

  @Override
  public ListEx<ValidationError> getValidationErrors() {
    return property.getModelPropertyAccessor().getValidationErrors();
  }

  @Override
  public Object getSelectedItem() {
    return property.getValue();
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  @Override
  public void setSelectedItem(Object anItem) {
    if (!property.setValueByConsumer((E) anItem)) {
      return;
    }

    property.optionsProperty.getList().fireItemChanged(null);
  }
}
