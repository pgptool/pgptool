package ru.skarpushin.swingpm.modelprops.virtualtable;

import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;

public interface ModelVirtualTablePropertyAccessor<E> extends ModelTablePropertyAccessor<E> {
  ModelPropertyAccessor<Boolean> getHasData();
}
