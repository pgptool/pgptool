package org.pgptool.gui.ui.tools.swingpm;

import java.awt.Dialog.ModalityType;
import java.awt.Image;
import java.awt.Window;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.swing.JDialog;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.WindowIcon;
import org.pgptool.gui.ui.tools.geometrymemory.WindowGeometryPersister;
import org.pgptool.gui.ui.tools.geometrymemory.WindowGeometryPersisterImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import ru.skarpushin.swingpm.base.DialogViewBase;
import ru.skarpushin.swingpm.base.PresentationModel;

public abstract class DialogViewBaseEx<TPM extends PresentationModel> extends DialogViewBase<TPM>
    implements InitializingBean {

  @Autowired protected ScheduledExecutorService scheduledExecutorService;
  @Autowired protected ConfigPairs uiGeom;

  protected WindowGeometryPersister windowGeometryPersister;

  @Override
  public void afterPropertiesSet() {
    super.afterPropertiesSet();
  }

  @Override
  protected void showDialog(Window optionalParent) {
    UiUtils.centerWindow(dialog, optionalParent);

    super.showDialog(optionalParent);

    if (dialog.getModalityType() == ModalityType.MODELESS) {
      UiUtils.makeSureWindowBroughtToFront(dialog);
    }
  }

  public static int spacing(int lettersCount) {
    return UiUtils.getFontRelativeSize(lettersCount);
  }

  @Override
  protected List<Image> getWindowIcon() {
    return WindowIcon.getWindowIcon();
  }

  protected void initWindowGeometryPersister(JDialog dialog, String key) {
    windowGeometryPersister =
        new WindowGeometryPersisterImpl(dialog, key, uiGeom, scheduledExecutorService);
    if (dialog.isResizable()) {
      if (!windowGeometryPersister.restoreSize()) {
        dialog.pack();
      }
    }
  }
}
