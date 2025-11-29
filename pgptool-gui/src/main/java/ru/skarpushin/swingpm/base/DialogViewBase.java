package ru.skarpushin.swingpm.base;

import com.google.common.base.Preconditions;
import java.awt.Container;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import org.pgptool.gui.ui.tools.UiUtils;

public abstract class DialogViewBase<TPM extends PresentationModel> extends ViewBase<TPM>
    implements HasWindow {
  private static final KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
  private static final String dispatchWindowClosingActionMapKey =
      "ru.skarpushin.swingpm.base.DialogViewBase:WINDOW_CLOSING";

  protected JDialog dialog;

  private final WindowAdapter windowAdapter;

  public DialogViewBase() {
    windowAdapter = buildWindowAdapter();
  }

  protected WindowAdapter buildWindowAdapter() {
    return new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        super.windowClosing(e);
        if (isAttached()) {
          dispatchWindowCloseEvent(UiUtils.actionEvent(e.getSource(), "windowClosing"));
        }
      }
    };
  }

  private final ComponentListener componentAdapter =
      new ComponentAdapter() {
        @Override
        public void componentHidden(ComponentEvent e) {}

        @Override
        public void componentShown(ComponentEvent e) {
          handleDialogShown();
        }
      };

  public static void installEscapeCloseOperation(final Window window, JRootPane rootPane) {
    Action dispatchClosing =
        new AbstractAction() {
          @Override
          public void actionPerformed(ActionEvent event) {
            window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
          }
        };
    rootPane
        .getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(escapeStroke, dispatchWindowClosingActionMapKey);
    rootPane.getActionMap().put(dispatchWindowClosingActionMapKey, dispatchClosing);
  }

  @Override
  protected abstract void internalInitComponents();

  @Override
  protected void internalRenderTo(Container owner, Object constraints) {
    Preconditions.checkArgument(
        owner == null || owner instanceof Window,
        "Target must not be specified or be sub-calss of Window");
    Preconditions.checkState(pm != null, "PM is required for this view");

    if (isDialogMustBeReinitialized(owner, constraints)) {
      tearDownPreviousDialogInstance();
    }

    if (dialog == null) {
      dialog = initDialog((Window) owner, constraints);
      Preconditions.checkState(dialog != null, "Dialog failed to initialize");

      dialog.addComponentListener(componentAdapter);
      dialog.addWindowListener(windowAdapter);
      installEscapeCloseOperation(dialog, dialog.getRootPane());
      initWindowIcon();
    }

    Window optionalParent = owner instanceof Window ? (Window) owner : null;
    showDialog(optionalParent);
  }

  protected List<Image> getWindowIcon() {
    return null;
  }

  protected void initWindowIcon() {
    List<Image> images = getWindowIcon();
    if (images == null) {
      return;
    }
    dialog.setIconImages(images);
  }

  protected void showDialog(Window optionalParent) {
    UiUtils.centerWindow(dialog, optionalParent);

    // NOTE: IMPORTANT: This is modal dialog, so this call blocks further
    // execution!!!
    dialog.setVisible(true);
  }

  protected void tearDownPreviousDialogInstance() {
    Preconditions.checkState(dialog != null);

    internalUnrender();

    dialog.remove(getRootPanel());
    dialog.dispose();
    dialog = null;
  }

  protected boolean isDialogMustBeReinitialized(Container owner, Object constraints) {
    return dialog != null && dialog.getOwner() != owner;
  }

  @Override
  protected void internalUnrender() {
    if (dialog == null || !dialog.isVisible()) {
      return;
    }

    dialog.setVisible(false);
  }

  /** This called when window is opened. Might be overrided by subclass */
  protected void handleDialogShown() {}

  protected abstract JDialog initDialog(Window owner, Object constraints);

  protected abstract JPanel getRootPanel();

  protected abstract void dispatchWindowCloseEvent(ActionEvent originAction);

  @Override
  public Window getWindow() {
    return dialog;
  }
}
