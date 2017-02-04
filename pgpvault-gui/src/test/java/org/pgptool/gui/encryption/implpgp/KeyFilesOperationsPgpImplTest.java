package org.pgptool.gui.encryption.implpgp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.implpgp.KeyDataPgp;
import org.pgptool.gui.encryption.implpgp.KeyFilesOperationsPgpImpl;

import integr.org.pgptool.gui.TestTools;

public class KeyFilesOperationsPgpImplTest {
	private static String tempFolder;

	@BeforeClass
	public static void beforeAll() {
		tempFolder = TestTools.getTempDir();
	}

	@AfterClass
	public static void afterAll() throws IOException {
		FileUtils.deleteDirectory(new File(tempFolder));
	}

	private KeyFilesOperationsPgpImpl buildFixture() {
		KeyFilesOperationsPgpImpl ret = new KeyFilesOperationsPgpImpl();
		return ret;
	}

	@Test
	public void testReadKeyFromFileExpectCanReadPrivateKey() throws Exception {
		KeyFilesOperationsPgpImpl fixture = buildFixture();
		Key<KeyDataPgp> key = fixture.readKeyFromFile(TestTools.getFileNameForResource("keys/Alice.asc"));
		assertNotNull(key);
		assertNotNull(key.getKeyInfo());
		assertEquals("Alice <alice@email.com>", key.getKeyInfo().getUser());
	}

	@Test
	public void testCanExportAndImportSameKeyAsc() throws Exception {
		KeyFilesOperationsPgpImpl fixture = buildFixture();
		Key<KeyDataPgp> _key = fixture.readKeyFromFile(TestTools.getFileNameForResource("keys/Alice.asc"));
		String fileName = tempFolder + File.separator + "alice.asc";
		fixture.exportPrivateKey(_key, fileName);
		Key<KeyDataPgp> key = fixture.readKeyFromFile(fileName);
		assertNotNull(key);
		assertNotNull(key.getKeyInfo());
		assertEquals("Alice <alice@email.com>", key.getKeyInfo().getUser());
	}

	@Test
	public void testCanExportAndImportSameKeyBpg() throws Exception {
		KeyFilesOperationsPgpImpl fixture = buildFixture();
		Key<KeyDataPgp> _key = fixture.readKeyFromFile(TestTools.getFileNameForResource("keys/Alice.asc"));
		String fileName = tempFolder + File.separator + "alice.bpg";
		fixture.exportPrivateKey(_key, fileName);
		Key<KeyDataPgp> key = fixture.readKeyFromFile(fileName);
		assertNotNull(key);
		assertNotNull(key.getKeyInfo());
		assertEquals("Alice <alice@email.com>", key.getKeyInfo().getUser());
	}

	@Test
	public void testReadKeyFromFileExpectCanReadPublicKey() throws Exception {
		KeyFilesOperationsPgpImpl fixture = buildFixture();
		Key<KeyDataPgp> key = fixture.readKeyFromFile(TestTools.getFileNameForResource("keys/Paul.asc"));
		assertNotNull(key);
		assertNotNull(key.getKeyInfo());
		assertEquals("Paul <paul@email.com>", key.getKeyInfo().getUser());
	}

}
