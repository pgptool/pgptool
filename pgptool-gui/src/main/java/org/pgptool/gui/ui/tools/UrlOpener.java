package org.pgptool.gui.ui.tools;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;

public class UrlOpener {
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	public static LocalizedActionEx buildAction(String messageCode, String url, Object context) {
		return new LocalizedActionEx(messageCode, context) {
			private static final long serialVersionUID = 8967629059243288916L;

			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				open(e, url);
			}
		};
	}

	public static void open(ActionEvent actionEvent, String url) {
		if (url.contains("#")) {
			UrlOpener.openUrlWithHash(actionEvent, url);
		} else {
			UrlOpener.openUrlWithoutHash(actionEvent, url);
		}
	}

	private static void openUrlWithoutHash(ActionEvent actionEvent, String url) {
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser(actionEvent, "failed.toOpenBrowser", t);
		}
	}

	/**
	 * NOTE: {@link URI} will no work with URLs that have hash char. So we have to
	 * use this workaround
	 */
	private static void openUrlWithHash(ActionEvent actionEvent, String url) {
		try {
			String fileName = "url-" + url.hashCode() + ".html";
			File htmlFile = new File(FileUtils.getTempDirectory(), fileName);
			if (!htmlFile.exists()) {
				createHtmlFileWithRedirectTo(htmlFile, url);
				htmlFile.deleteOnExit();
			}
			Desktop.getDesktop().open(htmlFile);
		} catch (Throwable t) {
			EntryPoint.reportExceptionToUser(actionEvent, "failed.toOpenBrowser", t);
		}
	}

	private static void createHtmlFileWithRedirectTo(File htmlFile, String url) throws IOException {
		FileUtils.write(htmlFile, "<html><head><meta http-equiv=\"refresh\" content=\"0;url="
				+ StringEscapeUtils.escapeHtml4(url) + "\" /></head></html>", UTF_8, false);
	}

}
