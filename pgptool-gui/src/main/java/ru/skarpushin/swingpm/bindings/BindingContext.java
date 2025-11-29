package ru.skarpushin.swingpm.bindings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JToggleButton;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import ru.skarpushin.swingpm.base.HasValidationErrorsListEx;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.ModelPropertyReader;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultiSelInListPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.lists.ModelSelInComboBoxPropertyAccessor;

public class BindingContext {
  private final List<Binding> bindings = new ArrayList<>();

  protected BindingContext() {}

  /**
   * @deprecated avoid using this approach cause it uses hard coded literal of target method name.
   *     Use {@link #registerOnChangeHandler(ModelPropertyReader, TypedPropertyChangeListener)}
   *     instead
   */
  @Deprecated
  public Binding registerOnChangeHandler(
      ModelPropertyAccessor<?> property, Object targetObject, String methodName) {
    Binding ret = StaticBinding.registerOnChangeHandler(property, targetObject, methodName);
    bindings.add(ret);
    return ret;
  }

  public <T> Binding registerOnChangeHandler(
      ModelPropertyReader<T> property, TypedPropertyChangeListener<T> listener) {
    Binding ret = StaticBinding.registerOnChangeHandler(property, listener);
    bindings.add(ret);
    return ret;
  }

  public <T> Binding registerOnChangeHandler(
      ModelPropertyReader<T> property,
      TypedPropertyChangeListener<T> listener,
      boolean fireEventRightAfterSubscription) {
    Binding ret = StaticBinding.registerOnChangeHandler(property, listener);
    bindings.add(ret);
    if (fireEventRightAfterSubscription) {
      T value = property.getValue();
      listener.handlePropertyChanged(this, property.getPropertyName(), value, value);
    }
    return ret;
  }

  public Binding registerPropertyValuePropagation(
      ModelPropertyReader<?> property, Object targetObject, String targetProperty) {
    Binding ret =
        StaticBinding.registerPropertyValuePropagation(property, targetObject, targetProperty);
    bindings.add(ret);
    return ret;
  }

  public void unbindAll() {
    for (Iterator<Binding> iter = bindings.iterator(); iter.hasNext(); ) {
      Binding b = iter.next();
      if (b.isBound()) {
        b.unbind();
      }

      iter.remove();
    }
  }

  public Binding registerTextPropertyBinding(
      ModelPropertyAccessor<String> property, Document targetObject) {
    Binding ret = StaticBinding.registerTextPropertyBinding(property, targetObject);
    bindings.add(ret);
    return ret;
  }

  public void add(Binding ret) {
    bindings.add(ret);
  }

  public void setupBinding(
      ModelPropertyAccessor<String> textProperty, JTextComponent textComponent) {
    registerTextPropertyBinding(textProperty, textComponent.getDocument());
    createValidationErrorsViewIfAny(textProperty, textComponent);
  }

  public void createValidationErrorsViewIfAny(
      HasValidationErrorsListEx validationErrorsSource, JComponent component) {
    if (validationErrorsSource.getValidationErrors() == null) {
      return;
    }

    constructValidationErrorsBinding(validationErrorsSource, component);
  }

  protected void constructValidationErrorsBinding(
      HasValidationErrorsListEx validationErrorsSource, JComponent component) {
    // NOTE: Subclass may want to override
  }

  public <E> void setupBinding(
      ModelSelInComboBoxPropertyAccessor<E> comboBoxModel, JComboBox<E> comboBox) {
    add(new ModelSelInComboBoxBinding(this, comboBoxModel, comboBox));
  }

  public <E> void setupBinding(
      ModelSelInComboBoxPropertyAccessor<E> comboBoxModel, JList<E> singleSelList) {
    add(new ModelSelInListBinding(this, comboBoxModel, singleSelList));
  }

  public <E> void setupBinding(
      ModelMultiSelInListPropertyAccessor<E> modelProperty, JList<E> list) {
    add(new ModelMultSelInListBinding<>(this, modelProperty, list));
  }

  public void setupBinding(ModelPropertyReader<String> stringProperty, JLabel label) {
    add(JLabelBinding.forStringProperty(this, stringProperty, label));
  }

  public <T> void setupBinding(
      ModelPropertyReader<T> property, JLabel label, Function<T, String> converter) {
    add(new JLabelBinding<>(this, property, label, converter));
  }

  public void setupBinding(Action action, AbstractButton trigger) {
    add(new ActionBinding(action, trigger));
  }

  public void setupBinding(
      Action optionalAction,
      ModelPropertyAccessor<Boolean> booleanProperty,
      JToggleButton toggleButton) {
    add(new ToggleButtonBinding(optionalAction, booleanProperty, toggleButton));
  }
}
