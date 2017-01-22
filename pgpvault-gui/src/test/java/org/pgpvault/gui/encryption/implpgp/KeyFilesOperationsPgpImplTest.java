package org.pgpvault.gui.encryption.implpgp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.pgpvault.gui.encryption.api.dto.Key;

import integr.org.pgpvault.gui.TestTools;

public class KeyFilesOperationsPgpImplTest {
	private KeyFilesOperationsPgpImpl buildFixture() {
		KeyFilesOperationsPgpImpl ret = new KeyFilesOperationsPgpImpl();
		return ret;
	}

	@Test
	public void testReadKeyFromFileExpectCanReadPrivateKey() throws Exception {
		KeyFilesOperationsPgpImpl fixture = buildFixture();
		Key<KeyDataPgp> key = fixture.readKeyFromFile(TestTools.getFileNameForResource("Alice.asc"));
		assertNotNull(key);
		assertNotNull(key.getKeyInfo());
		assertEquals("Alice <alice@email.com>", key.getKeyInfo().getUser());
	}

	@Test
	public void testReadKeyFromFileExpectCanReadPublicKey() throws Exception {
		KeyFilesOperationsPgpImpl fixture = buildFixture();
		Key<KeyDataPgp> key = fixture.readKeyFromFile(TestTools.getFileNameForResource("Paul.asc"));
		assertNotNull(key);
		assertNotNull(key.getKeyInfo());
		assertEquals("Paul <paul@email.com>", key.getKeyInfo().getUser());
	}

}
