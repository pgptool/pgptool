package ru.skarpushin.swingpm.bindings;

import com.google.common.base.Preconditions;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import ru.skarpushin.swingpm.modelprops.lists.ModelSelInComboBoxPropertyAccessor;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ModelSelInComboBoxBinding implements Binding {
  private JComboBox comboBox;

  public ModelSelInComboBoxBinding(
      BindingContext ctx, ModelSelInComboBoxPropertyAccessor<?> comboBoxModel, JComboBox comboBox) {
    this.comboBox = comboBox;

    comboBox.setModel(comboBoxModel);
    ctx.createValidationErrorsViewIfAny(comboBoxModel, comboBox);
  }

  @Override
  public boolean isBound() {
    return comboBox != null;
  }

  @Override
  public void unbind() {
    Preconditions.checkState(comboBox != null);

    comboBox.setModel(new DefaultComboBoxModel());
    comboBox = null;
  }
}
