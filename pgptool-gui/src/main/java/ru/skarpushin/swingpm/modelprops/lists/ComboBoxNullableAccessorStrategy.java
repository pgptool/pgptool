package ru.skarpushin.swingpm.modelprops.lists;

import java.util.Objects;
import javax.swing.event.ListDataListener;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;

public class ComboBoxNullableAccessorStrategy<E> implements ModelSelInComboBoxPropertyAccessor<E> {
  private final ModelSelInComboBoxProperty<E> property;
  private final E nullItem;

  public ComboBoxNullableAccessorStrategy(ModelSelInComboBoxProperty<E> property, E nullItem) {
    this.property = property;
    this.nullItem = nullItem;
  }

  @Override
  public E get(int idx) {
    if (idx == property.optionsAccessor.getSize()) {
      return null;
    }

    return property.optionsAccessor.get(idx);
  }

  @Override
  public int indexOf(E item) {
    if (item == null) {
      return property.optionsAccessor.getSize();
    }
    return property.optionsAccessor.indexOf(item);
  }

  @Override
  public int getSize() {
    return property.optionsAccessor.getSize() + 1;
  }

  @Override
  public E getElementAt(int index) {
    if (index == property.optionsAccessor.getSize()) {
      return nullItem;
    }

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
  public E getSelectedItem() {
    if (property.hasValue()) {
      return property.getValue();
    }

    return nullItem;
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  @Override
  public void setSelectedItem(Object anItem) {
    E valueToSet = Objects.equals(anItem, nullItem) ? null : (E) anItem;

    if (!property.setValueByConsumer((E) valueToSet)) {
      return;
    }

    property.optionsProperty.getList().fireItemChanged(null);
  }
}
