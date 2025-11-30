package EXPORT.org.summerb;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class ExceptionTranslatorDelegatingImpl implements ExceptionTranslator {
  protected final List<ExceptionTranslator> translators;
  protected ExceptionUnwindingStrategy exceptionUnwindingStrategy =
      new ExceptionUnwindingStrategyImpl();
  protected String joinerString = " -> ";

  public ExceptionTranslatorDelegatingImpl(List<ExceptionTranslator> translators) {
    this.translators = new LinkedList<>(translators);
  }

  @Override
  public String buildUserMessage(Throwable t, Locale locale) {
    if (t == null) {
      return "";
    }

    StringBuilder ret = new StringBuilder();

    Throwable cur = exceptionUnwindingStrategy.getNextMeaningfulExc(t);
    while (cur != null) {
      if (!ret.isEmpty()) {
        appendJoiner(ret);
      }

      boolean matchFound = false;
      for (ExceptionTranslator translator : translators) {
        String msg = translator.buildUserMessage(cur, locale);
        if (msg == null) {
          continue;
        }

        ret.append(msg);
        matchFound = true;
        break;
      }
      if (!matchFound) {
        ret.append(t.getMessage());
      }

      if (cur == cur.getCause()) {
        break;
      }
      cur = exceptionUnwindingStrategy.getNextMeaningfulExc(cur.getCause());
    }
    return ret.toString();
  }

  /**
   * Add joiner between messages. Do not add part of the {@link #joinerString} that is similar to
   * the current ending
   */
  protected void appendJoiner(StringBuilder ret) {
    if ("".equals(joinerString)) {
      return;
    }

    int pos1 = Math.max(0, ret.length() - joinerString.length());
    int pos2 = 0;

    for (; pos1 < ret.length(); pos1++) {
      char charSubject = ret.charAt(pos1);
      char charExpected = joinerString.charAt(pos2);
      if (charSubject == charExpected) {
        break;
      }
    }

    for (int i = pos1; pos2 < joinerString.length() && i < ret.length(); pos2++, i++) {
      char charSubject = ret.charAt(i);
      char charExpected = joinerString.charAt(pos2);
      if (charSubject != charExpected) {
        break;
      }
    }

    if (pos2 == 0) {
      ret.append(joinerString);
    } else if (pos2 == joinerString.length()) {
    } else {
      ret.append(joinerString.substring(pos2));
    }
  }

  public ExceptionUnwindingStrategy getExceptionUnwindingStrategy() {
    return exceptionUnwindingStrategy;
  }

  public void setExceptionUnwindingStrategy(ExceptionUnwindingStrategy exceptionUnwindingStrategy) {
    this.exceptionUnwindingStrategy = exceptionUnwindingStrategy;
  }

  public String getJoinerString() {
    return joinerString;
  }

  public void setJoinerString(String joinerString) {
    this.joinerString = joinerString;
  }
}
