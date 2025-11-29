package ru.skarpushin.swingpm.bindings;

public interface ValueChangeListener<TSource, TValue> {
  void onChanged(TSource source, String propertyName, TValue oldValue, TValue newValue);
}
