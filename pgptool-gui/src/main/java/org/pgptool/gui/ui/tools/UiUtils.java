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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

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
					// TODO: That doesn't actually affect defaults. Need to find out how to fix it
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

	public static void messageBox(Component parent, String msg, String title, int messageType) {
		if (msg.length() > 100) {
			JOptionPane.showMessageDialog(parent, getMultilineMessage(msg), title, messageType);
		} else {
			JOptionPane.showMessageDialog(parent, msg, title, messageType);
		}
	}

	public static JComponent getMultilineMessage(String msg) {
		JXLabel lbl = new JXLabel(msg);
		lbl.setLineWrap(true);
		lbl.setMaxLineSpan(getFontRelativeSize(30));
		return lbl;
	}

	public static void reportExceptionToUser(String errorMessageCode, Throwable cause, Object... messageArgs) {
		EntryPoint.reportExceptionToUser(errorMessageCode, cause, messageArgs);
	}
}
