package org.pgpvault.gui.tools;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PathUtilsTest {

	@Test
	public void testExtractBasePath() {
		// NOTE: Nut sure how to write one test which will work on both
		// platforms =(
		if (System.getProperty("os.name").toLowerCase().contains("windows")) {
			String result = PathUtils.extractBasePath("c:\\temp\\aaa.docx");
			assertEquals("c:\\temp", result);
		} else {
			String result = PathUtils.extractBasePath("/var/opt/file.txt");
			assertEquals("/var/opt", result);
		}
	}

}
