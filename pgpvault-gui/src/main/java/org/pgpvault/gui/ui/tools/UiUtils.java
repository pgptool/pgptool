package org.pgpvault.gui.ui.tools;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.StringEscapeUtils;
import org.pgpvault.gui.app.Messages;

public class UiUtils {
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

	public static boolean confirm(String userPromptMessageCode, Window parent) {
		int response = JOptionPane.showConfirmDialog(parent, Messages.get(userPromptMessageCode),
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

}
