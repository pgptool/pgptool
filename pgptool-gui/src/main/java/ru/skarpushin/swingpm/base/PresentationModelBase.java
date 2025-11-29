package ru.skarpushin.swingpm.base;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JDialog;
import javax.swing.JFrame;
import org.pgptool.gui.ui.root.RootPm;
import org.pgptool.gui.ui.tools.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <H> type of host
 * @param <P> type of init params. Could be Void
 */
public class PresentationModelBase<H, P> implements PresentationModel {
  protected Logger log = LoggerFactory.getLogger(getClass());

  /** Action that resulted in invocation of this PresentationModelBase */
  protected ActionEvent originAction;

  protected H host;
  protected P initParams;
  protected List<View<?>> views = new ArrayList<>();

  @Override
  public boolean isAttached() {
    return false;
  }

  @Override
  public void detach() {}

  @Override
  public void registerView(View<?> view) {
    views.add(view);
  }

  @Override
  public void unregisterView(View<?> view) {
    views.remove(view);
  }

  /**
   * Search registered views for registered Window. Normally this is will be used as a parent for
   * modal dialog
   */
  public Window findRegisteredWindowIfAny() {
    for (View<?> view : views) {
      if (view instanceof HasWindow) {
        return ((HasWindow) view).getWindow();
      }
    }

    if (originAction != null) {
      Window ret = UiUtils.findWindow(originAction);
      if (ret != null && ret.isVisible()) {
        log.debug(
            "findRegisteredWindowIfAny returning window from originAction: {}", getWindowName(ret));
        return ret;
      }
    }

    Window ret = RootPm.INSTANCE.findMainFrameWindow();
    if (ret != null && ret.isVisible()) {
      log.debug("findRegisteredWindowIfAny returning window from rootPm: {}", getWindowName(ret));
      return ret;
    }

    log.debug("findRegisteredWindowIfAny returning null");
    return null;
  }

  @SuppressWarnings("unchecked")
  protected <T> List<T> findViewsWhichImplements(Class<T> clazz) {
    List<T> ret = new LinkedList<>();

    for (View<?> view : views) {
      if (clazz.isAssignableFrom(view.getClass())) {
        ret.add((T) view);
      }
    }

    return ret;
  }

  private String getWindowName(Window ret) {
    if (ret instanceof JFrame) {
      return ((JFrame) ret).getTitle();
    }
    if (ret instanceof JDialog) {
      return ((JDialog) ret).getTitle();
    }
    return ret.getName();
  }

  /**
   * Init presenter by clarifying which action resulted in its invocation (used to trace origin
   * action and subsequently origin window)
   *
   * @return true if presenter is ok to proceed, or false otherwise
   */
  public boolean init(ActionEvent originAction, H host, P initParams) {
    this.originAction = originAction;
    this.host = host;
    this.initParams = initParams;
    return true;
  }
}
