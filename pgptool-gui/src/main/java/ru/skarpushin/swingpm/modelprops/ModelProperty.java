package ru.skarpushin.swingpm.modelprops;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.SwingUtilities;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.FilterPredicate;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.tools.edt.Edt;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

/**
 * Convenient class to hold property by property owner
 *
 * @author sergeyk
 */
public class ModelProperty<E> implements FilterPredicate<ValidationError> {
  // private static Logger log = LoggerFactory.getLogger(ModelProperty.class);

  protected final Object source;
  protected final String propertyName;
  protected final ValueAdapter<E> valueAdapter;
  protected ListEx<ValidationError> validationErrors;
  private final ModelPropertyAccessorImpl modelPropertyAccessor = new ModelPropertyAccessorImpl();
  private final PropertyChangeSupport propertyChangeSupport;
  private boolean fireEventsInEventDispatchingThread = true;

  public ModelProperty(Object source, ValueAdapter<E> valueAdapter, String propertyName) {
    this(source, valueAdapter, propertyName, null);
  }

  public ModelProperty(
      Object source,
      ValueAdapter<E> valueAdapter,
      String propertyName,
      ListEx<ValidationError> veSource) {
    Preconditions.checkArgument(valueAdapter != null);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(propertyName));

    this.source = source;
    this.valueAdapter = valueAdapter;
    this.propertyName = propertyName;

    if (veSource != null) {
      validationErrors = veSource.getView(this);
    }

    this.propertyChangeSupport = new PropertyChangeSupport(source);
  }

  @Override
  public boolean isSuitable(ValidationError subject) {
    return propertyName.equals(subject.getPropertyName());
  }

  /**
   * Intended to be used by property owner to set property value
   *
   * @return true if value was updated, false if value left unchanged (no events fired)
   */
  public boolean setValueByOwner(E value) {
    E oldValue = valueAdapter.getValue();
    if (isSameValue(oldValue, value)) {
      return false;
    }

    valueAdapter.setValue(value);
    firePropertyChanged(oldValue, value);
    return true;
  }

  /**
   * Intended to be overridden to handle changes came from consumer (normally - UI layer)
   *
   * @return true if value was updated, false if value left unchanged (no events fired)
   * @deprecated USE IT ONLY FROM CLASS WHICH OWNS THIS PROPERTY!!! Not actually deprecated, want to
   *     avoid misuse.
   */
  @Deprecated
  public boolean setValueByConsumer(E value) {
    return setValueByOwner(value);
  }

  private boolean isSameValue(E a, E b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return a.equals(b);
  }

  public void firePropertyChanged(E oldValue, E value) {
    final PropertyChangeEvent evt = new PropertyChangeEvent(source, propertyName, oldValue, value);

    if (isShouldDefferEventDispatchToEdtThread()) {
      try {
        // Would be really nice to get rid of blocking nature of this part
        // But need to carefully consider data flow here.
        Edt.invokeOnEdtAndWait(() -> propertyChangeSupport.firePropertyChange(evt));
        // Edt.invokeOnEdtAsync(() -> propertyChangeSupport.firePropertyChange(evt));
      } catch (Throwable e) {
        throw new RuntimeException("Failed to fire property " + propertyName + " change", e);
      }
    } else {
      propertyChangeSupport.firePropertyChange(evt);
    }
  }

  public void firePropertyChanged() {
    firePropertyChanged(null, valueAdapter.getValue());
  }

  private boolean isShouldDefferEventDispatchToEdtThread() {
    return fireEventsInEventDispatchingThread && !SwingUtilities.isEventDispatchThread();
  }

  public E getValue() {
    return valueAdapter.getValue();
  }

  public String getPropertyName() {
    return propertyName;
  }

  /**
   * @return an interface for getting access to and modifying value
   */
  public ModelPropertyAccessor<E> getModelPropertyAccessor() {
    return modelPropertyAccessor;
  }

  /**
   * @return an interface for getting access to value but without ability to modify it
   */
  public ModelPropertyReader<E> getModelPropertyReader() {
    return modelPropertyAccessor;
  }

  public boolean isFireEventsInEventDispatchingThread() {
    return fireEventsInEventDispatchingThread;
  }

  public void setFireEventsInEventDispatchingThread(boolean fireEventsInEventDispatchingThread) {
    this.fireEventsInEventDispatchingThread = fireEventsInEventDispatchingThread;
  }

  public boolean hasValue() {
    return getValue() != null;
  }

  public ListEx<ValidationError> getValidationErrors() {
    return validationErrors;
  }

  private class ModelPropertyAccessorImpl
      implements ModelPropertyAccessor<E>, ModelPropertyReader<E> {
    @Override
    public E getValue() {
      return ModelProperty.this.getValue();
    }

    @Override
    public void setValue(E value) {
      setValueByConsumer(value);
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
      propertyChangeSupport.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener propertyChangeBoundHandler) {
      propertyChangeSupport.removePropertyChangeListener(propertyChangeBoundHandler);
    }

    @Override
    public String getPropertyName() {
      return ModelProperty.this.getPropertyName();
    }

    @Override
    public ListEx<ValidationError> getValidationErrors() {
      return validationErrors;
    }
  }
}
