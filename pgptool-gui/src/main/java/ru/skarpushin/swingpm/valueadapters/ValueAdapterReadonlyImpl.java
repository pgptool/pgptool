package ru.skarpushin.swingpm.valueadapters;

/**
 * Adapter which actually holds value but reuses to change it
 *
 * @author sergeyk
 * @param <E>
 */
public class ValueAdapterReadonlyImpl<E> implements ValueAdapter<E> {
  private E value;

  public ValueAdapterReadonlyImpl() {}

  public ValueAdapterReadonlyImpl(E initialValue) {
    this.value = initialValue;
  }

  public static <E> ValueAdapterReadonlyImpl<E> build(E value) {
    return new ValueAdapterReadonlyImpl<>(value);
  }

  @Override
  public E getValue() {
    return value;
  }

  @Override
  public void setValue(E value) {
    throw new IllegalStateException("Operation not supported");
  }
}
