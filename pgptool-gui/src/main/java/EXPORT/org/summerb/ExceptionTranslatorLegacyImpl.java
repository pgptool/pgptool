package EXPORT.org.summerb;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.MessageSource;

public class ExceptionTranslatorLegacyImpl extends ExceptionTranslatorDelegatingImpl {
  public ExceptionTranslatorLegacyImpl(MessageSource messageSource) {
    super(buildLegacyTranslatorsList(messageSource));
  }

  public static List<ExceptionTranslator> buildLegacyTranslatorsList(MessageSource messageSource) {
    return Arrays.asList(
        new ExceptionTranslatorFveImpl(messageSource),
        new ExceptionTranslatorHasMessageImpl(messageSource),
        new ExceptionTranslatorClassNameImpl(messageSource));
  }
}
