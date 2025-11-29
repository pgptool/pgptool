package EXPORT.org.summerb;

import java.util.Locale;

/**
 * Impl of this interface supposed to translate exception into human language and be represented by
 * string.so that we can show it to user
 *
 * @author sergeyk
 */
public interface ExceptionTranslator {
  /**
   * Translate exception into user locale using provided messageSource
   *
   * @return message ready for user OR null if this translator doesn't support this type of
   *     exception
   */
  String buildUserMessage(Throwable t, Locale locale);
}
