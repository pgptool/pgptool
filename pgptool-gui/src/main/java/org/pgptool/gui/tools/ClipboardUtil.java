package org.pgptool.gui.tools;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import org.apache.log4j.Logger;

public class ClipboardUtil {
	private static Logger log = Logger.getLogger(ClipboardUtil.class);

	public static String tryGetClipboardText() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
		if (!hasTransferableText) {
			return null;
		}

		try {
			return (String) contents.getTransferData(DataFlavor.stringFlavor);
		} catch (UnsupportedFlavorException | IOException ex) {
			log.warn("Can't get clipboard text contents", ex);
			return null;
		}
	}

	public static void setClipboardText(String string) {
		StringSelection stringSelection = new StringSelection(string);
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(stringSelection, null);
	}
}
