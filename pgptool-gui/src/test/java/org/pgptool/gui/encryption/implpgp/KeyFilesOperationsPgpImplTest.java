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
