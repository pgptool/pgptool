package ru.skarpushin.swingpm.base;

public interface Detachable {

  /**
   * Is attached to something outer?
   *
   * @return true if there is something (normally event subsciption, or some timer which
   *     periodically triggers some operation)
   */
  boolean isAttached();

  /** Detach from any external dependency or stop any backgroung processing/working */
  void detach();
}
