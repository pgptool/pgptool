package ru.skarpushin.swingpm.valueadapters;

/**
 * Adapter which actually holds value
 *
 * @author sergeyk
 * @param <E>
 */
public class ValueAdapterHolderImpl<E> implements ValueAdapter<E> {
  private E value;

  public ValueAdapterHolderImpl() {}

  public ValueAdapterHolderImpl(E initialValue) {
    this.value = initialValue;
  }

  @Override
  public E getValue() {
    return value;
  }

  @Override
  public void setValue(E value) {
    this.value = value;
  }
}
