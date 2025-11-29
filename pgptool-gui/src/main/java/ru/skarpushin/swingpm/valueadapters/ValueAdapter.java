package ru.skarpushin.swingpm.valueadapters;

/**
 * Impl can define the way how value is set or retrieved. It might be mapped to POJO using
 * reflection or it might hold actuall value
 *
 * @author sergeyk
 * @param <E>
 */
public interface ValueAdapter<E> {
  E getValue();

  void setValue(E value);
}
