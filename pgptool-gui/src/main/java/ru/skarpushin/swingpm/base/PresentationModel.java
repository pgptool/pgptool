package ru.skarpushin.swingpm.base;

public interface PresentationModel extends Detachable {
  /**
   * View is expected to be registered within PM using this method. It's not required.
   *
   * @param view
   */
  void registerView(View<?> view);

  /**
   * View will be unregistered within PM
   *
   * @param view
   */
  void unregisterView(View<?> view);
}
