package org.pgpvault.gui.ui.tools;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.pgpvault.gui.app.Messages;

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

	public static JTextComponent buildMultilineLabel(String labelText) {
		// JXLabel lblx = new JXLabel(labelText);
		// lblx.setLineWrap(true);
		// //lblx.setMaxLineSpan(100);
		// return lblx;

		JLabel lbl = new JLabel();

		JTextArea ret = new JTextArea(labelText);
		ret.setBorder(BorderFactory.createEmptyBorder());
		ret.setEditable(false);
		ret.setLineWrap(true);
		ret.setBackground(lbl.getBackground());
		ret.setForeground(lbl.getForeground());
		ret.setFont(lbl.getFont());

		return ret;
	}

	public static boolean confirm(String userPromptMessageCode, Object[] messageArgs, Window parent) {
		int response = JOptionPane.showConfirmDialog(parent, Messages.get(userPromptMessageCode, messageArgs),
				Messages.get("term.confirmation"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

		return response == JOptionPane.OK_OPTION;
	}

	public static String promptUserForTextString(String fieldLabel, Object[] fieldLabelMsgArgs, String windowTitle,
			Window window) {
		String ret = JOptionPane.showInputDialog(window, Messages.get(fieldLabel, fieldLabelMsgArgs),
				Messages.get(windowTitle), JOptionPane.QUESTION_MESSAGE);

		return ret == null ? null : ret.trim();
	}

	public static void showDuplicateNameValidationErrorMessage(Window parent) {
		JOptionPane.showMessageDialog(parent, Messages.get("validation.entityWithSameNameExists"),
				Messages.get("term.validationError"), JOptionPane.ERROR_MESSAGE);
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
					fixFontSize();
				} catch (Throwable t) {
					log.error("Failed to set L&F", t);
				}
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
	 * Hack to make sure window is visible. On windows it's sometimes created
	 * but on a background. User can see "flashing" icon in a task bar but
	 * window stays on a background.
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
	}

}
