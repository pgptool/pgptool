package ru.skarpushin.swingpm.tools.edt;

/**
 * Class to perform safe edt invocations to avoid freezng and deadlocking
 *
 * @author sergey.karpushin
 */
public interface EdtInvoker {
  void invoke(Runnable task);
}
