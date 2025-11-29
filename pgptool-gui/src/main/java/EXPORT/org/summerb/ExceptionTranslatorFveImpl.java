package EXPORT.org.summerb;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.summerb.i18n.I18nUtils;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationException;

public class ExceptionTranslatorFveImpl implements ExceptionTranslator {
  protected MessageSource messageSource;

  public ExceptionTranslatorFveImpl(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @Override
  public String buildUserMessage(Throwable t, Locale locale) {
    if (!ValidationException.class.equals(t.getClass())) {
      return null;
    }
    ValidationException fve = (ValidationException) t;

    StringBuilder ret = new StringBuilder();
    ret.append(I18nUtils.buildMessage(fve, messageSource, locale));
    ret.append(": ");
    boolean first = true;
    for (ValidationError ve : fve.getErrors()) {
      if (!first) {
        ret.append(", ");
      }
      ret.append(translateFieldName(ve.getPropertyName(), messageSource, locale));
      ret.append(" - ");
      ret.append(I18nUtils.buildMessage(ve, messageSource, locale));
      first = false;
    }
    return ret.toString();
  }

  protected static Object translateFieldName(
      String fieldToken, MessageSource messageSource, Locale locale) {
    try {
      return messageSource.getMessage(fieldToken, null, locale);
    } catch (NoSuchMessageException nfe) {
      try {
        return messageSource.getMessage("term." + fieldToken, null, locale);
      } catch (NoSuchMessageException nfe2) {
        return fieldToken;
      }
    }
  }
}
