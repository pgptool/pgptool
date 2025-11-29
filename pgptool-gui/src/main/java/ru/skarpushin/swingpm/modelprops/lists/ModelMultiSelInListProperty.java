package ru.skarpushin.swingpm.modelprops.lists;

import java.util.ArrayList;
import java.util.List;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.collections.ListExEventListener;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

/**
 * Represents model for multiple selection in list of optionsAccessor.
 *
 * <p>This model is not complete solution because it's heavily depends on view how it will bind this
 * model to it' components and listers.
 *
 * @author sergeyk
 * @param <E>
 */
public class ModelMultiSelInListProperty<E> extends ModelListProperty<E> {
  private final ModelListProperty<E> options;

  private final ListExEventListener<E> optionsListChangesHandler =
      new ListExEventListener<>() {
        @Override
        public void onItemAdded(E item, int atIndex) {
          // ignore
        }

        @Override
        public void onItemChanged(E item, int atIndex) {
          // ignore
        }

        @Override
        public void onItemRemoved(E item, int wasAtIndex) {
          getList().remove(item);
        }

        @Override
        public void onAllItemsRemoved(int sizeWas) {
          getList().clear();
        }
      };
  private final ModelMultiSelInListPropertyAccessor<E> modelMultiSelInListPropertyAccessor =
      new ModelMultiSelInListPropertyAccessor<>() {
        @Override
        public ModelListPropertyAccessor<E> getOptions() {
          return options.getModelListPropertyAccessor();
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setNewSelection(List<E> selectedObjects) {
          if (selectedObjects == null || selectedObjects.isEmpty()) {
            getList().clear();
            return;
          }

          List<E> newSelection = new ArrayList<>(selectedObjects);

          // modify our list according to selected indices
          for (int i = getList().size() - 1; i >= 0; i--) {
            E prevSelItem = getList().get(i);

            if (!newSelection.remove(prevSelItem)) {
              // if were unable to remove it from new selection, that mean
              // it's not there
              // log.debug("setNewSelection(): remove()");
              getList().remove(i);
            } else {
              // the item in new sel and in old... no changes
            }
          }

          // if newSel is not empty - than we have new items
          for (E item : newSelection) {
            if (!options.getList().contains(item)) {
              // Should we throw an error in that cae?...
              continue;
            }
            getList().add(item);
          }
        }

        @Override
        public ModelListPropertyAccessor<E> getSelectionAccessor() {
          return ModelMultiSelInListProperty.this.getModelListPropertyAccessor();
        }

        @Override
        public void addListExEventListener(ListExEventListener<E> l) {
          getList().addListExEventListener(l);
        }

        @Override
        public void removeListExEventListener(ListExEventListener<E> l) {
          getList().removeListExEventListener(l);
        }

        @Override
        public ListEx<ValidationError> getValidationErrors() {
          return getModelListPropertyAccessor().getValidationErrors();
        }
      };

  public ModelMultiSelInListProperty(
      Object source,
      ValueAdapter<List<E>> valueAdapter,
      String propertyName,
      ModelListProperty<E> options) {
    this(source, valueAdapter, propertyName, options, null);
  }

  public ModelMultiSelInListProperty(
      Object source,
      ValueAdapter<List<E>> valueAdapter,
      String propertyName,
      ModelListProperty<E> options,
      ListEx<ValidationError> veSource) {
    super(source, valueAdapter, propertyName, veSource);
    this.options = options;

    // Monitor changes in parent list
    options.getList().addListExEventListener(optionsListChangesHandler);
  }

  public ModelMultiSelInListPropertyAccessor<E> getModelMultiSelInListPropertyAccessor() {
    return modelMultiSelInListPropertyAccessor;
  }
}
