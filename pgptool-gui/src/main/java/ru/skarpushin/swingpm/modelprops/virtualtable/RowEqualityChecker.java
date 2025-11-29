package ru.skarpushin.swingpm.modelprops.virtualtable;

/**
 * This interface needed if special equality algorithm for rtows needs to be used. This is to avoid
 * violating Object.equals() contract
 *
 * @param <E>
 */
public interface RowEqualityChecker<E> {
  public boolean areEquals(E o1, E o2);
}
