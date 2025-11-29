package ru.skarpushin.swingpm.tools.edt;

import javax.swing.SwingUtilities;

public class Edt {
  private static EdtInvoker edtInvoker = new EdtInvokerSimpleImpl();

  public static EdtInvoker getEdtInvoker() {
    return edtInvoker;
  }

  public static void setEdtInvoker(EdtInvoker newValue) {
    edtInvoker = newValue;
  }

  public static void invokeOnEdtAsync(Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      SwingUtilities.invokeLater(runnable);
    }
  }

  public static void invokeOnEdtAndWait(Runnable runnable) {
    if (SwingUtilities.isEventDispatchThread()) {
      runnable.run();
    } else {
      getEdtInvoker().invoke(runnable);
    }
  }
}
