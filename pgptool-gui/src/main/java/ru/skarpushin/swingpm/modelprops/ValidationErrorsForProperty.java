package ru.skarpushin.swingpm.modelprops;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.summerb.validation.ValidationError;
import ru.skarpushin.swingpm.collections.FilterPredicate;

public class ValidationErrorsForProperty implements FilterPredicate<ValidationError> {

  private final String fieldToken;

  public ValidationErrorsForProperty(String fieldToken) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(fieldToken));

    this.fieldToken = fieldToken;
  }

  @Override
  public boolean isSuitable(ValidationError subject) {
    return subject.getPropertyName().equals(fieldToken);
  }
}
