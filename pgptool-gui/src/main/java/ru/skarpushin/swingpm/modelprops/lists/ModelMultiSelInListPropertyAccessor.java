package ru.skarpushin.swingpm.modelprops.lists;

import java.util.List;
import ru.skarpushin.swingpm.base.HasValidationErrorsListEx;
import ru.skarpushin.swingpm.collections.HasListExEvents;

public interface ModelMultiSelInListPropertyAccessor<E>
    extends HasListExEvents<E>, HasValidationErrorsListEx {
  /** Return all optionsAccessor we can choose from. Normally used to populate list */
  ModelListPropertyAccessor<E> getOptions();

  /** Return managed list accessor which holds selection stateId */
  ModelListPropertyAccessor<E> getSelectionAccessor();

  void setNewSelection(List<E> selectedObjects);
}
