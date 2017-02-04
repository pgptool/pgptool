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
package integr.org.pgptool.gui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import org.pgptool.gui.encryption.implpgp.KeyFilesOperationsPgpImplTest;

public class TestTools {
	private static String tempDir;

	public static String getFileNameForResource(String fileName) throws URISyntaxException {
		URL resource = KeyFilesOperationsPgpImplTest.class.getResource("/" + fileName);
		assertNotNull("Cannot load resource " + fileName, resource);
		String ret = new File(resource.toURI()).getAbsolutePath();
		assertTrue("Cannot confirm file existance", new File(ret).exists());
		return ret;
	}

	public static String getTempDir() {
		if (tempDir == null) {
			tempDir = buildNewTempDir();
		}
		return buildNewTempDir();
	}

	public static String buildNewTempDir() {
		final String baseTempPath = System.getProperty("java.io.tmpdir");

		Random rand = new Random();
		int randomInt = 1 + rand.nextInt();

		File tempDir = new File(baseTempPath + File.separator + "tempDir" + randomInt);
		if (tempDir.exists() == false) {
			tempDir.mkdir();
		}

		tempDir.deleteOnExit();
		return tempDir.getAbsolutePath();
	}
}
