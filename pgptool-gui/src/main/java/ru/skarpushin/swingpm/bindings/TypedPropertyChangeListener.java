package ru.skarpushin.swingpm.bindings;

public interface TypedPropertyChangeListener<T> {
  void handlePropertyChanged(Object source, String propertyName, T oldValue, T newValue);
}
