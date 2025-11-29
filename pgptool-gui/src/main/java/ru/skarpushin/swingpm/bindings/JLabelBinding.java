package ru.skarpushin.swingpm.bindings;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Function;
import javax.swing.JLabel;
import ru.skarpushin.swingpm.modelprops.ModelPropertyReader;

public class JLabelBinding<T> implements Binding, PropertyChangeListener {
  private static final Function<String, String> STRING_CONVERTER = s -> s == null ? "" : s;
  // private static Logger log = LoggerFactory.getLogger(JLabelBinding.class);

  private final ModelPropertyReader<T> property;
  private final Function<T, String> converter;
  private JLabel label;

  public JLabelBinding(
      BindingContext bindingContext,
      ModelPropertyReader<T> property,
      JLabel label,
      Function<T, String> converter) {
    this.converter = converter;
    this.property = property;
    this.label = label;

    property.addPropertyChangeListener(this);
    //    label.setText(property.getValue() == null ? "" : property.getValue());
    label.setText(converter.apply(property.getValue()));
  }

  public static JLabelBinding<String> forStringProperty(
      BindingContext bindingContext, ModelPropertyReader<String> property, JLabel label) {
    return new JLabelBinding<>(bindingContext, property, label, STRING_CONVERTER);
  }

  @Override
  public boolean isBound() {
    return label != null;
  }

  @Override
  public void unbind() {
    property.removePropertyChangeListener(this);
    label = null;
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (label == null) {
      return;
    }

    //    label.setText(property.getValue() == null ? "" : property.getValue());
    label.setText(converter.apply(property.getValue()));
  }
}
