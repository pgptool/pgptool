package org.pgptool.gui.tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Scanner;

public class TextFile {
	private static final String DEFAULT_ENCODING = "UTF-8";

	public static String read(String fileName) throws Exception {
		StringBuilder text = new StringBuilder();
		String NL = System.getProperty("line.separator");
		Scanner scanner = new Scanner(new FileInputStream(fileName), DEFAULT_ENCODING);
		try {
			while (scanner.hasNextLine()) {
				text.append(scanner.nextLine() + NL);
			}
		} finally {
			scanner.close();
		}
		return text.toString();
	}

	public static void write(String fileName, String configContents) throws Exception {
		Writer out = new OutputStreamWriter(new FileOutputStream(fileName), DEFAULT_ENCODING);
		try {
			out.write(configContents);
		} finally {
			out.close();
		}
	}

}
