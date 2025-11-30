package EXPORT.org.summerb;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * This translator treats class name as a message code, and its message as a first argument message
 *
 * @author sergeyk
 */
public class ExceptionTranslatorClassNameImpl implements ExceptionTranslator {
  protected final MessageSource messageSource;

  public ExceptionTranslatorClassNameImpl(MessageSource messageSource2) {
    messageSource = messageSource2;
  }

  @Override
  public String buildUserMessage(Throwable t, Locale locale) {
    try {
      String className = t.getClass().getName();
      return messageSource.getMessage(className, new Object[] {t.getMessage()}, locale);
    } catch (NoSuchMessageException nfe) {
      return t.getClass().getSimpleName() + " (" + t.getLocalizedMessage() + ")";
    }
  }
}
