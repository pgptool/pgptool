/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.pgptool.gui.ui.tools;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.imported.JXLabel;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import ru.skarpushin.swingpm.tools.edt.Edt;

public class UiUtils {
  public static Logger log = Logger.getLogger(UiUtils.class);

  /**
   * By default window will be placed at 0x0 coordinates, which is not pretty. We have to position
   * it to screen center
   *
   * @param subject the window to position
   * @param optionalOrigin The window where the action to open subject originated from
   */
  public static void centerWindow(Window subject, Window optionalOrigin) {
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    List<GraphicsDevice> graphicsDevices = Arrays.asList(ge.getScreenDevices());

    if (graphicsDevices.size() == 1) {
      // Single monitor configuration -- easy-peasy
      Dimension scrDim = Toolkit.getDefaultToolkit().getScreenSize();
      Point location = offsetWindowLocation(subject, scrDim.width / 2, scrDim.height / 2);
      subject.setLocation(location);
      return;
    }

    // Multi-monitor configuration -- not a rocket science too
    log.debug(
        "Positioning window according to multi-monitor configuration: "
            + formatMonitorSizes(graphicsDevices));
    Pair<GraphicsDevice, GraphicsConfiguration> device = null;
    if (optionalOrigin != null) {
      device = determineGraphicsDeviceByWindow(ge, graphicsDevices, optionalOrigin);
      if (device != null && log.isDebugEnabled()) {
        int index = graphicsDevices.indexOf(device.getLeft());
        log.debug(
            "Parent window "
                + format(optionalOrigin.getBounds())
                + " belongs to screen "
                + format(index, device.getLeft(), device.getRight()));
      }
    }

    if (device == null) {
      device =
          Pair.of(
              ge.getDefaultScreenDevice(), ge.getDefaultScreenDevice().getDefaultConfiguration());
      int index = graphicsDevices.indexOf(device.getLeft());
      log.debug("Selecting default screen: " + format(index, device.getLeft(), device.getRight()));
    }

    Rectangle bounds = device.getRight().getBounds();
    Point location =
        offsetWindowLocation(subject, bounds.x + bounds.width / 2, bounds.y + bounds.height / 2);
    subject.setLocation(location);
  }

  private static Point offsetWindowLocation(Window frm, int centerPointX, int centerPointY) {
    return new Point(
        centerPointX - frm.getSize().width / 2, centerPointY - frm.getSize().height / 2);
  }

  private static Pair<GraphicsDevice, GraphicsConfiguration> determineGraphicsDeviceByWindow(
      GraphicsEnvironment graphicsEnvironment,
      List<GraphicsDevice> graphicsDevices,
      Window window) {
    log.debug("determineGraphicsDeviceByWindow " + format(window.getBounds()));

    Rectangle windowBounds = window.getBounds();
    int lastArea = 0;
    Pair<GraphicsDevice, GraphicsConfiguration> ret = null;
    for (int i = 0; i < graphicsDevices.size(); ++i) {
      GraphicsDevice graphicsDevice = graphicsDevices.get(i);
      log.debug(
          "- Checking GraphicsDevice: "
              + format(i, graphicsDevice, graphicsDevice.getDefaultConfiguration()));
      GraphicsConfiguration[] graphicsConfigurations = graphicsDevice.getConfigurations();
      Set<Rectangle> seen = new HashSet<>();
      for (int j = 0; j < graphicsConfigurations.length; ++j) {
        GraphicsConfiguration graphicsConfiguration = graphicsConfigurations[j];
        Rectangle graphicsBounds = graphicsConfiguration.getBounds();
        if (!seen.add(graphicsBounds)) {
          continue;
        }

        log.debug("  - Checking GraphicsConfiguration: " + format(graphicsBounds));
        Rectangle intersection = windowBounds.intersection(graphicsBounds);
        int area = intersection.width * intersection.height;
        if (area != 0 && area > lastArea) {
          lastArea = area;
          ret = Pair.of(graphicsDevice, graphicsConfiguration);
        }
      }
    }

    return ret;
  }

  private static String formatMonitorSizes(List<GraphicsDevice> graphicsDevices) {
    String ret = "";
    for (int i = 0; i < graphicsDevices.size(); i++) {
      if (i > 0) {
        ret += ", ";
      }

      GraphicsDevice graphicsDevice = graphicsDevices.get(i);
      ret += format(i, graphicsDevice, graphicsDevice.getDefaultConfiguration());
    }
    return ret;
  }

  private static String format(
      int index, GraphicsDevice graphicsDevice, GraphicsConfiguration graphicsConfiguration) {
    Rectangle bounds = graphicsConfiguration.getBounds();
    return "" + index + ": " + format(bounds);
  }

  private static String format(Rectangle bounds) {
    return "x="
        + (int) bounds.getX()
        + ",y="
        + (int) bounds.getY()
        + ",w="
        + (int) bounds.getWidth()
        + ",h="
        + (int) bounds.getHeight();
  }

  public static String plainToBoldHtmlString(String text) {
    return "<html><body><b>" + StringEscapeUtils.escapeHtml4(text) + "</b></body></html>";
  }

  public static String envelopeStringIntoHtml(String text) {
    return "<html><body>" + StringEscapeUtils.escapeHtml4(text) + "</body></html>";
  }

  public static boolean confirmRegular(
      ActionEvent originEvent, String userPromptMessageCode, Object[] messageArgs) {
    return confirm(originEvent, userPromptMessageCode, messageArgs, JOptionPane.QUESTION_MESSAGE);
  }

  public static boolean confirmWarning(
      ActionEvent originEvent, String userPromptMessageCode, Object[] messageArgs) {
    return confirm(originEvent, userPromptMessageCode, messageArgs, JOptionPane.WARNING_MESSAGE);
  }

  private static boolean confirm(
      ActionEvent originEvent, String userPromptMessageCode, Object[] messageArgs, int severity) {
    int response = JOptionPane.OK_OPTION;

    String msg = Messages.get(userPromptMessageCode, messageArgs);
    if (msg.length() > 70) {
      response =
          JOptionPane.showConfirmDialog(
              findWindow(originEvent),
              getMultilineMessage(msg),
              Messages.get("term.confirmation"),
              JOptionPane.OK_CANCEL_OPTION,
              severity);
    } else {
      response =
          JOptionPane.showConfirmDialog(
              findWindow(originEvent),
              msg,
              Messages.get("term.confirmation"),
              JOptionPane.OK_CANCEL_OPTION,
              severity);
    }
    return response == JOptionPane.OK_OPTION;
  }

  public static String promptUserForTextString(
      ActionEvent originEvent, String windowTitle, String fieldLabel, Object[] fieldLabelMsgArgs) {
    String ret =
        JOptionPane.showInputDialog(
            findWindow(originEvent),
            Messages.get(fieldLabel, fieldLabelMsgArgs),
            Messages.get(windowTitle),
            JOptionPane.QUESTION_MESSAGE);

    return ret == null ? null : ret.trim();
  }

  public static int getFontRelativeSize(int size) {
    StringBuilder sb = new StringBuilder(size);
    for (int i = 0; i < size; i++) {
      sb.append("W");
    }
    JLabel lbl = new JLabel(sb.toString());
    return (int) lbl.getPreferredSize().getWidth();
  }

  public static void setLookAndFeel() {
    // NOTE: We doing it this way to prevent dead=locks that is sometimes
    // happens if do it in main thread
    Edt.invokeOnEdtAndWait(
        new Runnable() {
          @Override
          public void run() {
            try {
              UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
              fixCheckBoxMenuItemForeground();
              fixFontSize();
            } catch (Throwable t) {
              log.error("Failed to set L&F", t);
            }
          }

          /**
           * In some cases (depends on OS theme) check menu item foreground is same as background -
           * thus it's invisible when checked
           */
          private void fixCheckBoxMenuItemForeground() {
            UIDefaults defaults = UIManager.getDefaults();
            Color selectionForeground = defaults.getColor("CheckBoxMenuItem.selectionForeground");
            Color foreground = defaults.getColor("CheckBoxMenuItem.foreground");
            Color background = defaults.getColor("CheckBoxMenuItem.background");
            if (colorsDiffPercentage(selectionForeground, background) < 10) {
              // TBD: That doesn't actually affect defaults. Need to find out how to fix it
              defaults.put("CheckBoxMenuItem.selectionForeground", foreground);
            }
          }

          private int colorsDiffPercentage(Color c1, Color c2) {
            int diffRed = Math.abs(c1.getRed() - c2.getRed());
            int diffGreen = Math.abs(c1.getGreen() - c2.getGreen());
            int diffBlue = Math.abs(c1.getBlue() - c2.getBlue());

            float pctDiffRed = (float) diffRed / 255;
            float pctDiffGreen = (float) diffGreen / 255;
            float pctDiffBlue = (float) diffBlue / 255;

            return (int) ((pctDiffRed + pctDiffGreen + pctDiffBlue) / 3 * 100);
          }

          private void fixFontSize() {
            if (isJreHandlesScaling()) {
              log.info("JRE handles font scaling, won't change it");
              return;
            }
            log.info("JRE doesnt't seem to support font scaling");

            Toolkit toolkit = Toolkit.getDefaultToolkit();
            int dpi = toolkit.getScreenResolution();
            if (dpi == 96) {
              if (log.isDebugEnabled()) {
                Font font = UIManager.getDefaults().getFont("TextField.font");
                String current = font != null ? Integer.toString(font.getSize()) : "unknown";
                log.debug(
                    "Screen dpi seem to be 96. Not going to change font size. Btw current size seem to be "
                        + current);
              }
              return;
            }
            int targetFontSize = 12 * dpi / 96;
            log.debug("Screen dpi = " + dpi + ", decided to change font size to " + targetFontSize);
            setDefaultSize(targetFontSize);
          }

          private boolean isJreHandlesScaling() {
            try {
              JreVersion noNeedToScaleForVer = JreVersion.parseString("9");
              String jreVersionStr = System.getProperty("java.version");
              if (jreVersionStr != null) {
                JreVersion curVersion = JreVersion.parseString(jreVersionStr);
                if (noNeedToScaleForVer.compareTo(curVersion) <= 0) {
                  return true;
                }
              }

              return false;
            } catch (Throwable t) {
              log.warn(
                  "Failed to see oif JRE can handle font scaling. Will assume it does. JRE version: "
                      + System.getProperty("java.version"),
                  t);
              return true;
            }
          }

          public void setDefaultSize(int size) {
            Set<Object> keySet = UIManager.getLookAndFeelDefaults().keySet();
            Object[] keys = keySet.toArray(new Object[keySet.size()]);
            for (Object key : keys) {
              if (key != null && key.toString().toLowerCase().contains("font")) {
                Font font = UIManager.getDefaults().getFont(key);
                if (font != null) {
                  Font changedFont = font.deriveFont((float) size);
                  UIManager.put(key, changedFont);
                  Font doubleCheck = UIManager.getDefaults().getFont(key);
                  log.debug(
                      "Font size changed for "
                          + key
                          + ". From "
                          + font.getSize()
                          + " to "
                          + doubleCheck.getSize());
                }
              }
            }
          }
        });
    log.info("L&F set");
  }

  /**
   * Hack to make sure window is visible. On windows it's sometimes created but on a background.
   * User can see "flashing" icon in a task bar but window stays on a background.
   *
   * <p>PRESUMING: setVisible(true) was already called
   *
   * <p>More on this here:
   * http://stackoverflow.com/questions/309023/how-to-bring-a-window-to-the-front
   */
  public static void makeSureWindowBroughtToFront(Window window) {
    window.setAlwaysOnTop(true);
    window.toFront();
    window.requestFocus();
    window.setAlwaysOnTop(false);
    window.repaint();

    // NOTE: Don't bother trying to use Desktop.getDesktop().requestForeground(); --
    // this seem to be supported on Android only
  }

  public static void messageBox(
      ActionEvent originEvent,
      String messageText,
      String messageTitle,
      MessageSeverity messageSeverity) {
    int messageType = JOptionPane.INFORMATION_MESSAGE;
    if (messageSeverity == MessageSeverity.ERROR) {
      messageType = JOptionPane.ERROR_MESSAGE;
    } else if (messageSeverity == MessageSeverity.WARNING) {
      messageType = JOptionPane.WARNING_MESSAGE;
    } else if (messageSeverity == MessageSeverity.INFO) {
      messageType = JOptionPane.INFORMATION_MESSAGE;
    }
    UiUtils.messageBox(originEvent, messageText, messageTitle, messageType);
  }

  /**
   * @param messageType one of the JOptionPane ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE,
   *     QUESTION_MESSAGE, or PLAIN_MESSAGE
   */
  public static void messageBox(
      ActionEvent originEvent, String msg, String title, int messageType) {
    Object content = buildMessageContentDependingOnLength(msg);

    Component parent = findWindow(originEvent);
    if (messageType != JOptionPane.ERROR_MESSAGE) {
      JOptionPane.showMessageDialog(parent, content, title, messageType);
      return;
    }

    Object[] options = {text("action.ok"), text("phrase.saveMsgToFile")};
    if ("action.ok".equals(options[0])) {
      // if app context wasn't started MessageSource wont be available
      options = new String[] {"OK", "Save message to file"};
    }

    int result =
        JOptionPane.showOptionDialog(
            parent,
            content,
            title,
            JOptionPane.YES_NO_OPTION,
            messageType,
            null,
            options,
            JOptionPane.YES_OPTION);
    if (result == JOptionPane.YES_OPTION || result == JOptionPane.CLOSED_OPTION) {
      return;
    }

    // Save to file
    saveMessageToFile(parent, msg);
  }

  private static void saveMessageToFile(Component parent, String msg) {
    JFileChooser fileChooser = new JFileChooser();
    if (fileChooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    File file = fileChooser.getSelectedFile();
    try {
      FileUtils.write(file, msg, Charset.forName("UTF-8"), false);
    } catch (IOException e) {
      log.error("Failed to save error message to file: " + file, e);
      JOptionPane.showMessageDialog(
          parent, "Failed to save message to file", "Error", JOptionPane.ERROR_MESSAGE);
      // come on !!!
    }
  }

  private static Object buildMessageContentDependingOnLength(String msg) {
    Object content = "";
    if (msg.length() > 300 || msg.split("\n").length > 2) {
      content = getScrollableMessage(msg);
    } else if (msg.length() > 100) {
      content = getMultilineMessage(msg);
    } else {
      content = msg;
    }
    return content;
  }

  private static JScrollPane getScrollableMessage(String msg) {
    JTextArea textArea = new JTextArea(msg);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(false);
    textArea.setMargin(new Insets(5, 5, 5, 5));
    textArea.setFont(new JTextField().getFont()); // dirty fix to use better font
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setPreferredSize(new Dimension(700, 150));
    scrollPane.getViewport().setView(textArea);
    return scrollPane;
  }

  public static JComponent getMultilineMessage(String msg) {
    JXLabel lbl = new JXLabel(msg);
    lbl.setLineWrap(true);
    lbl.setMaxLineSpan(getFontRelativeSize(50));
    return lbl;
  }

  public static Window findWindow(ActionEvent event) {
    Window defaultValue = null;
    if (EntryPoint.rootPmStatic != null) {
      defaultValue = EntryPoint.rootPmStatic.findMainFrameWindow();
    }

    if (event == null) {
      return defaultValue;
    }
    Object source = event.getSource();
    if (source == null) {
      return defaultValue;
    }

    if (source instanceof Component) {
      Window containingWindow = findWindow((Component) source);
      if (containingWindow != null) {
        return containingWindow;
      }
    }

    if (source instanceof PresentationModelBaseEx) {
      Window view = ((PresentationModelBaseEx<?, ?>) source).findRegisteredWindowIfAny();
      if (view != null) {
        return view;
      }
    }

    return defaultValue;
  }

  public static Window findWindow(Component c) {
    Window defaultValue = null;
    if (EntryPoint.rootPmStatic != null) {
      defaultValue = EntryPoint.rootPmStatic.findMainFrameWindow();
    }
    if (c == null) {
      return defaultValue;
    } else if (c instanceof JPopupMenu) {
      Component invoker = ((JPopupMenu) c).getInvoker();
      if (invoker == null) {
        return defaultValue;
      }
      return SwingUtilities.getWindowAncestor(invoker);
    } else if (c instanceof Window) {
      return (Window) c;
    } else {
      return findWindow(c.getParent());
    }
  }

  public static ActionEvent actionEvent(Object source, Action action) {
    if (source == null) {
      return null;
    }
    return actionEvent(source, String.valueOf(action.getValue(Action.NAME)));
  }

  public static ActionEvent actionEvent(Object source, String actionName) {
    if (source == null) {
      return null;
    }
    return new ActionEvent(source, ActionEvent.ACTION_PERFORMED, actionName);
  }

  public static ActionEvent actionEvent(PropertyChangeEvent evt) {
    if (evt.getSource() == null) {
      return null;
    }
    // NOTE: This is flawed. Currently, event source is what was hard-coded in
    // PresentationModel upon propertyModel init.
    // We should be propagating source from Binding and
    // ModelProperty.setValueByConsumer
    return actionEvent(evt.getSource(), evt.getPropertyName() + " property changed");
  }
}
