package ru.skarpushin.swingpm.modelprops;

import ru.skarpushin.swingpm.base.HasValidationErrorsListEx;

// ValueAdapter<E>

public interface ModelPropertyAccessor<E>
    extends ModelPropertyReader<E>, HasValidationErrorsListEx {

  void setValue(E value);
}
