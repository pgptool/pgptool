package org.pgptool.gui.bkgoperation;

public class ProgressHandlerNoOpImpl implements ProgressHandler {
  @Override
  public void onProgressUpdated(Progress progress) {
    // does nothing on purpose
  }
}
