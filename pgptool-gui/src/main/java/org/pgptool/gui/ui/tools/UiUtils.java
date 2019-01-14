/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
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
 *******************************************************************************/
package org.pgptool.gui.ui.tools;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXLabel;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.app.Messages;

import ru.skarpushin.swingpm.tools.edt.Edt;

public class UiUtils {
	public static Logger log = Logger.getLogger(UiUtils.class);

	public static void centerWindow(Window frm) {
		Dimension scrDim = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (scrDim.width - frm.getSize().width) / 2;
		int y = (scrDim.height - frm.getSize().height) / 2;
		frm.setLocation(x, y);
	}

	public static String plainToBoldHtmlString(String text) {
		return "<html><body><b>" + StringEscapeUtils.escapeXml(text) + "</b></body></html>";
	}

	public static String envelopeStringIntoHtml(String text) {
		return "<html><body>" + StringEscapeUtils.escapeXml(text) + "</body></html>";
	}

	public static boolean confirm(String userPromptMessageCode, Object[] messageArgs, Window parent) {
		int response = JOptionPane.OK_OPTION;

		String msg = Messages.get(userPromptMessageCode, messageArgs);
		if (msg.length() > 70) {
			response = JOptionPane.showConfirmDialog(parent, getMultilineMessage(msg),
					Messages.get("term.confirmation"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		} else {
			response = JOptionPane.showConfirmDialog(parent, msg, Messages.get("term.confirmation"),
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		}
		return response == JOptionPane.OK_OPTION;
	}

	public static String promptUserForTextString(String fieldLabel, Object[] fieldLabelMsgArgs, String windowTitle,
			Window window) {
		String ret = JOptionPane.showInputDialog(window, Messages.get(fieldLabel, fieldLabelMsgArgs),
				Messages.get(windowTitle), JOptionPane.QUESTION_MESSAGE);

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
		Edt.invokeOnEdtAndWait(new Runnable() {
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
			 * In some cases (depends on OS theme) check menu item foreground is same as
			 * bacground - thus it;'s invisible when cheked
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
					log.warn("Failed to see oif JRE can handle font scaling. Will assume it does. JRE version: "
							+ System.getProperty("java.version"), t);
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
							log.debug("Font size changed for " + key + ". From " + font.getSize() + " to "
									+ doubleCheck.getSize());
						}
					}
				}
			}
		});
		log.info("L&F set");
	}

	/**
	 * Hack to make sure window is visible. On windows it's sometimes created but on
	 * a background. User can see "flashing" icon in a task bar but window stays on
	 * a background.
	 * 
	 * PRESUMING: setVisible(true) was already called
	 * 
	 * More ion tihs jere:
	 * http://stackoverflow.com/questions/309023/how-to-bring-a-window-to-the-front
	 */
	public static void makeSureWindowBroughtToFront(Window window) {
		// int state = dialog.getExtendedState();
		// state &= ~JFrame.ICONIFIED;
		// dialog.setExtendedState(state);
		window.setAlwaysOnTop(true);
		window.toFront();
		window.requestFocus();
		window.setAlwaysOnTop(false);
		window.repaint();
	}

	public static void messageBox(String messageText, String messageTitle, MessageSeverity messageSeverity) {
		int messageType = JOptionPane.INFORMATION_MESSAGE;
		if (messageSeverity == MessageSeverity.ERROR) {
			messageType = JOptionPane.ERROR_MESSAGE;
		} else if (messageSeverity == MessageSeverity.WARNING) {
			messageType = JOptionPane.WARNING_MESSAGE;
		} else if (messageSeverity == MessageSeverity.INFO) {
			messageType = JOptionPane.INFORMATION_MESSAGE;
		}
		UiUtils.messageBox(null, messageText, messageTitle, messageType);
	}

	/**
	 * @param messageType
	 *            one of the JOptionPane ERROR_MESSAGE, INFORMATION_MESSAGE,
	 *            WARNING_MESSAGE, QUESTION_MESSAGE, or PLAIN_MESSAGE
	 */
	public static void messageBox(Component parent, String msg, String title, int messageType) {
		Object content = buildMessageContentDependingOnLength(msg);

		if (messageType != JOptionPane.ERROR_MESSAGE) {
			JOptionPane.showMessageDialog(parent, content, title, messageType);
			return;
		}

		Object[] options = { text("action.ok"), text("phrase.saveMsgToFile") };
		if ("action.ok".equals(options[0])) {
			// if app context wasn't started MessageSource wont be available
			options = new String[] { "OK", "Save message to file" };
		}

		int result = JOptionPane.showOptionDialog(parent, content, title, JOptionPane.YES_NO_OPTION, messageType, null,
				options, JOptionPane.YES_OPTION);
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
			JOptionPane.showMessageDialog(parent, "Failed to save message to file", "Error", JOptionPane.ERROR_MESSAGE);
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

	public static void reportExceptionToUser(String errorMessageCode, Throwable cause, Object... messageArgs) {
		EntryPoint.reportExceptionToUser(errorMessageCode, cause, messageArgs);
	}

}
