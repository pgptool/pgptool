package ru.skarpushin.swingpm.modelprops;

import java.beans.PropertyChangeListener;
import ru.skarpushin.swingpm.base.HasPropertyName;

public interface ModelPropertyReader<E> extends HasPropertyName {
  E getValue();

  // TBD: Add overload that allows to trigger event immediately with current value
  void addPropertyChangeListener(PropertyChangeListener propertyChangeListener);

  void removePropertyChangeListener(PropertyChangeListener propertyChangeBoundHandler);
}
