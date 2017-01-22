package integr.org.pgpvault.gui;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Random;

import org.pgpvault.gui.encryption.implpgp.KeyFilesOperationsPgpImplTest;

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
