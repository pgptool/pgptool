package org.pgpvault.gui.ui.tools;

import java.awt.Image;
import java.awt.Window;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

public class WindowIcon {
	protected static Logger log = Logger.getLogger(WindowIcon.class);

	private static List<Image> windowIcon;

	public static void setWindowIcon(Window window) {
		try {
			List<Image> icons = getWindowIcon();
			if (!icons.isEmpty()) {
				window.setIconImages(icons);
			}
		} catch (Throwable t) {
			log.error("Failed to set icon for: " + window, t);
		}
	}

	public static List<Image> getWindowIcon() {
		if (windowIcon == null) {
			ArrayList<Image> temp = new ArrayList<Image>();
			addImageIfFound(temp, "/icon-16.png");
			addImageIfFound(temp, "/icon-32.png");
			addImageIfFound(temp, "/icon-64.png");
			windowIcon = temp;
		}
		return windowIcon;
	}

	private static void addImageIfFound(ArrayList<Image> temp, String path) {
		Image image = loadImage(path);
		if (image != null) {
			temp.add(image);
		}
	}

	public static Image loadImage(String fileName) {
		URL resource = WindowIcon.class.getResource(fileName);
		if (resource == null) {
			log.error("Image not found: " + fileName);
			return null;
		}
		return new ImageIcon(resource).getImage();
	}

}
