package ru.skarpushin.swingpm.collections;

public interface FilterPredicate<T> {
  boolean isSuitable(T subject);
}
