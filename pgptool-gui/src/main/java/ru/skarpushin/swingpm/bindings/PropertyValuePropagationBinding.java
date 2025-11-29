package ru.skarpushin.swingpm.bindings;

import com.google.common.base.Preconditions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import ru.skarpushin.swingpm.modelprops.ModelPropertyReader;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

@SuppressWarnings("rawtypes")
public class PropertyValuePropagationBinding implements Binding, PropertyChangeListener {
  private final ModelPropertyReader<?> source;
  private ValueAdapter destination;

  @SuppressWarnings("unchecked")
  public PropertyValuePropagationBinding(ModelPropertyReader source, ValueAdapter destination) {
    Preconditions.checkArgument(source != null);
    Preconditions.checkArgument(destination != null);

    this.source = source;
    this.destination = destination;

    destination.setValue(source.getValue());

    source.addPropertyChangeListener(this);
  }

  @Override
  public boolean isBound() {
    return destination != null;
  }

  @Override
  public void unbind() {
    destination = null;
    source.removePropertyChangeListener(this);
  }

  @SuppressWarnings("unchecked")
  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (destination == null || !source.getPropertyName().equals(evt.getPropertyName())) {
      return;
    }

    try {
      // NOTE: Do not check if destination already has this value. If it
      // cares - it will do this check, we shouldn't care about it
      destination.setValue(evt.getNewValue());
    } catch (Throwable e) {
      throw new RuntimeException(
          "Failed to propagate "
              + source.getPropertyName()
              + " property value "
              + evt.getNewValue(),
          e);
    }
  }
}
