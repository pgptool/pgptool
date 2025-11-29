package ru.skarpushin.swingpm.modelprops.table;

import javax.swing.table.TableModel;
import ru.skarpushin.swingpm.base.HasPropertyName;
import ru.skarpushin.swingpm.base.HasValidationErrorsListEx;

public interface ModelTablePropertyAccessor<E>
    extends TableModel, HasPropertyName, HasValidationErrorsListEx {

  E findRowByIdx(int idx);

  int indexOf(E item);
}
