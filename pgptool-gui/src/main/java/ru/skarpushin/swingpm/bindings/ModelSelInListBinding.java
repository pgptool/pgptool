package ru.skarpushin.swingpm.bindings;

import com.google.common.base.Preconditions;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import ru.skarpushin.swingpm.modelprops.lists.ModelSelInComboBoxPropertyAccessor;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ModelSelInListBinding implements Binding, ListDataListener, ListSelectionListener {
  private final ModelSelInComboBoxPropertyAccessor<?> model;
  private JList list;

  public ModelSelInListBinding(
      BindingContext bindingContext, ModelSelInComboBoxPropertyAccessor<?> nodel, JList list) {
    this.model = nodel;
    this.list = list;
    Preconditions.checkArgument(bindingContext != null);
    Preconditions.checkArgument(nodel != null);
    Preconditions.checkArgument(list != null);
    Preconditions.checkArgument(list.getSelectionMode() == ListSelectionModel.SINGLE_SELECTION);

    list.setModel(nodel);
    list.setSelectedValue(nodel.getSelectedItem(), true);
    list.addListSelectionListener(this);
    nodel.addListDataListener(this);

    bindingContext.createValidationErrorsViewIfAny(model, list);
  }

  @Override
  public boolean isBound() {
    return list != null;
  }

  @Override
  public void unbind() {
    model.removeListDataListener(this);
    list.removeListSelectionListener(this);
    list.setModel(new DefaultListModel());
    list = null;
  }

  @Override
  public void intervalAdded(ListDataEvent e) {
    list.setSelectedValue(model.getSelectedItem(), true);
  }

  @Override
  public void intervalRemoved(ListDataEvent e) {
    list.setSelectedValue(model.getSelectedItem(), true);
  }

  @Override
  public void contentsChanged(ListDataEvent e) {
    list.setSelectedValue(model.getSelectedItem(), true);
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (e.getValueIsAdjusting() || !isBound()) {
      return;
    }

    model.setSelectedItem(list.getSelectedValue());
  }
}
