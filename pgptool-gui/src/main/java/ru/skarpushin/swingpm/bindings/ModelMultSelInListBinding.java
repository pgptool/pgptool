package ru.skarpushin.swingpm.bindings;

import com.google.common.base.Preconditions;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import ru.skarpushin.swingpm.collections.ListExEventListener;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultiSelInListPropertyAccessor;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ModelMultSelInListBinding<E>
    implements Binding, ListSelectionListener, ListExEventListener<E> {

  private ModelMultiSelInListPropertyAccessor<E> modelProperty;
  private JList<E> list;

  private boolean skipEventsFromList = false;

  public ModelMultSelInListBinding(
      BindingContext bindingContext,
      ModelMultiSelInListPropertyAccessor<E> modelProperty,
      JList<E> list) {
    this.modelProperty = modelProperty;
    this.list = list;

    list.setModel(modelProperty.getOptions());
    list.addListSelectionListener(this);
    modelProperty.addListExEventListener(this);
    bindingContext.createValidationErrorsViewIfAny(modelProperty, list);

    // Set initial selection
    // bypass events from list to avoid events cycling
    skipEventsFromList = true;
    try {
      updateListSelectionAccordingToModelState();
    } finally {
      skipEventsFromList = false;
    }
  }

  @Override
  public boolean isBound() {
    return modelProperty != null;
  }

  @Override
  public void unbind() {
    Preconditions.checkState(modelProperty != null);

    list.removeListSelectionListener(this);
    list.setModel(new DefaultListModel());
    modelProperty.removeListExEventListener(this);
    modelProperty = null;
    list = null;
  }

  @Override
  public void valueChanged(ListSelectionEvent arg0) {
    // log.debug("valueChanged(): adjusting = " + arg0.getValueIsAdjusting()
    // + ", selection: "+ Arrays.toString(list.getSelectedValues()));

    // Ignore event if selection is still being adjusted
    if (arg0.getValueIsAdjusting()) {
      return;
    }

    // bypass events from list to avoid events cycling
    skipEventsFromList = true;
    try {
      modelProperty.setNewSelection(list.getSelectedValuesList());
    } finally {
      skipEventsFromList = false;
    }
  }

  @Override
  public void onItemAdded(E item, int atIndex) {
    if (skipEventsFromList) {
      return;
    }

    // log.debug("onItemAdded(): " + item);
    int idx = modelProperty.getOptions().indexOf(item);
    list.getSelectionModel().addSelectionInterval(idx, idx);
  }

  @Override
  public void onItemChanged(E item, int atIndex) {
    if (skipEventsFromList) {
      return;
    }

    // log.debug("onItemChanged(): " + item);
  }

  @Override
  public void onItemRemoved(E item, int wasAtIndex) {
    if (skipEventsFromList) {
      return;
    }

    // log.debug("onItemRemoved(): " + item);
    int idx = modelProperty.getOptions().indexOf(item);
    list.getSelectionModel().removeSelectionInterval(idx, idx);
  }

  private void updateListSelectionAccordingToModelState() {
    list.setSelectedIndices(getCurrentSelectionIndicies());
  }

  private int[] getCurrentSelectionIndicies() {
    int[] ret = new int[modelProperty.getSelectionAccessor().getSize()];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = modelProperty.getOptions().indexOf(modelProperty.getSelectionAccessor().get(i));
    }
    return ret;
  }

  @Override
  public void onAllItemsRemoved(int sizeWas) {
    if (skipEventsFromList) {
      return;
    }

    // log.debug("onAllItemsRemoved(): " + sizeWas);
    list.clearSelection();
  }
}
