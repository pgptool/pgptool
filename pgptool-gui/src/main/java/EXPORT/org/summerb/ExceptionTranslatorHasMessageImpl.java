package EXPORT.org.summerb;

import java.util.Locale;
import org.springframework.context.MessageSource;
import org.summerb.i18n.HasMessageCode;
import org.summerb.i18n.I18nUtils;

public class ExceptionTranslatorHasMessageImpl implements ExceptionTranslator {
  protected final MessageSource messageSource;

  public ExceptionTranslatorHasMessageImpl(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  @Override
  public String buildUserMessage(Throwable t, Locale locale) {
    if (!HasMessageCode.class.isAssignableFrom(t.getClass())) {
      return null;
    }
    HasMessageCode hasMessage = (HasMessageCode) t;
    return I18nUtils.buildMessage(hasMessage, messageSource, locale);
  }
}
