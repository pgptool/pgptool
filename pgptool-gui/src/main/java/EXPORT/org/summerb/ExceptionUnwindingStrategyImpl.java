package EXPORT.org.summerb;

import java.util.Arrays;
import java.util.List;

public class ExceptionUnwindingStrategyImpl implements ExceptionUnwindingStrategy {
  // NOTE: I'm using String here because in some cases not all classes will be
  // available, i.e. excluded from classpath. Don
  public static final List<String> CLASS_NAMES_TO_SKIP =
      Arrays.asList(
          "jakarta.servlet.ServletException",
          "org.springframework.web.util.NestedServletException",
          "java.lang.reflect.UndeclaredThrowableException",
          "java.lang.reflect.InvocationTargetException");

  @Override
  public Throwable getNextMeaningfulExc(Throwable current) {
    if (current == null) {
      return null;
    }
    Throwable cur = current;
    while (cur != null && isShouldSkipException(cur)) {
      if (cur == cur.getCause()) {
        break;
      }
      cur = cur.getCause();
    }
    return cur;
  }

  protected boolean isShouldSkipException(Throwable cur) {
    return CLASS_NAMES_TO_SKIP.contains(cur.getClass().getName());
  }
}
