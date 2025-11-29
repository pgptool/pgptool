package EXPORT.org.summerb;

import java.lang.reflect.UndeclaredThrowableException;

public interface ExceptionUnwindingStrategy {
  /**
   * Get first exception that actually might mean something. Impl supposed to skip meaningless
   * exceptions like jakarta.servlet.ServletException, NestedServletException, {@link
   * UndeclaredThrowableException}, etc...
   *
   * @param current current exception at hand
   * @return either the same exception or more meaningful.
   */
  Throwable getNextMeaningfulExc(Throwable current);
}
