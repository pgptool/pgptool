package ru.skarpushin.swingpm.base;

import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.ListEx;

public interface HasValidationErrorsListEx {
  ListEx<ValidationError> getValidationErrors();
}
