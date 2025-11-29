package ru.skarpushin.swingpm.modelprops.lists;

import javax.swing.ListModel;
import ru.skarpushin.swingpm.base.HasPropertyName;
import ru.skarpushin.swingpm.base.HasValidationErrorsListEx;

public interface ModelListPropertyAccessor<E>
    extends ListModel<E>, HasPropertyName, HasValidationErrorsListEx {

  E get(int idx);

  int indexOf(E item);
}
