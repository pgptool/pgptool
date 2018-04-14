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
package integr.org.pgptool.gui.encryption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.bind.ValidationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.tools.TextFile;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.annotation.SystemProfileValueSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import integr.org.pgptool.gui.TestTools;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:integr-test-context.xml")
@ProfileValueSourceConfiguration(SystemProfileValueSource.class)
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class EncryptionDecryptionTests {
	@Autowired
	private KeyFilesOperations keyFilesOperations;
	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private EncryptionService encryptionService;
	@Autowired
	private KeyGeneratorService keyGeneratorService;
	@Autowired
	private String tempDirPath;

	private Map<String, Key> keys = new HashMap<>();
	private String testSubjectFilename;
	private String testSubjectContents;

	@Before
	public void prepareForTests() throws Exception {
		testSubjectFilename = TestTools.getFileNameForResource("testsubject.txt");
		testSubjectContents = TextFile.read(testSubjectFilename);

		importKeyFromResources("Alice.asc");
		importKeyFromResources("Bob.asc");
		importKeyFromResources("John.asc");
		importKeyFromResources("Paul.asc");
	}

	@After
	public void cleanUpTempDir() throws Exception {
		FileUtils.deleteDirectory(new File(tempDirPath));
	}

	private Key importKeyFromResources(String keyFileName) throws ValidationException, URISyntaxException {
		Key key = keyFilesOperations.readKeyFromFile(TestTools.getFileNameForResource("keys/" + keyFileName));
		keyRingService.addKey(key);
		keys.put(keyFileName, key);
		return key;
	}

	@Test
	public void testWeCanDecryptTheProductOfEncryption() throws Exception {
		String targetFilename = tempDirPath + File.separator + FilenameUtils.getBaseName(testSubjectFilename) + ".pgp";
		encryptionService.encrypt(testSubjectFilename, targetFilename, keys.values(), null, null, null);

		PasswordDeterminedForKey keyAndPassword = buildPasswordDeterminedForKey(targetFilename, "Alice.asc", "pass");

		encryptionService.decrypt(targetFilename, targetFilename + ".test", keyAndPassword, null, null);
		String result = TextFile.read(targetFilename + ".test");
		assertEquals(testSubjectContents, result);
	}

	private PasswordDeterminedForKey buildPasswordDeterminedForKey(String encryptedFile, String keyName,
			String password) {
		Set<String> decryptionKeys = encryptionService.findKeyIdsForDecryption(encryptedFile);
		Key key = keys.get(keyName);
		Optional<String> requestedKeyId = decryptionKeys.stream().filter(x -> key.getKeyData().isHasAlternativeId(x))
				.findFirst();
		assertTrue(requestedKeyId.isPresent());
		return new PasswordDeterminedForKey<>(requestedKeyId.get(), key, password);
	}

	@Test
	public void testWeCanDecryptTheProductOfEncryptionUsingCreatedKey() throws Exception {
		Key key = keyGeneratorService.createNewKey(KeyRingServiceTest.buildTestKey());
		List keys = Arrays.asList(key);

		String targetFilename = tempDirPath + File.separator + FilenameUtils.getBaseName(testSubjectFilename) + ".pgp";
		encryptionService.encrypt(testSubjectFilename, targetFilename, keys, null, null, null);

		String decryptionKeyId = (String) encryptionService.findKeyIdsForDecryption(targetFilename).iterator().next();
		PasswordDeterminedForKey keyAndPassword = new PasswordDeterminedForKey<>(decryptionKeyId, key, "pass");

		encryptionService.decrypt(targetFilename, targetFilename + ".test", keyAndPassword, null, null);
		String result = TextFile.read(targetFilename + ".test");
		assertEquals(testSubjectContents, result);
	}

	@Test
	public void testDecryptBinary() throws Exception {
		decryptionTestRoutine("encr-testsubject.txt.pgp");
	}

	@Test
	public void testDecryptArmored() throws Exception {
		decryptionTestRoutine("encr-testsubject.txt.asc");
	}

	@Test
	public void testDecryptSigned() throws Exception {
		decryptionTestRoutine("encr-sign-testsubject.txt.pgp");
	}

	@Test
	public void testDecryptSignedArmored() throws Exception {
		decryptionTestRoutine("encr-sign-testsubject.txt.asc");
	}

	@Test
	public void testDecryptSigned3() throws Exception {
		decryptionTestRoutine("encr-sign3-testsubject.txt.pgp");
	}

	private void decryptionTestRoutine(String sourceFile) throws Exception {
		String targetFilename = tempDirPath + File.separator + FilenameUtils.getBaseName(sourceFile);

		String encryptedFile = TestTools.getFileNameForResource("encrypted/" + sourceFile);
		PasswordDeterminedForKey keyAndPassword = buildPasswordDeterminedForKey(encryptedFile, "Alice.asc", "pass");
		encryptionService.decrypt(encryptedFile, targetFilename, keyAndPassword, null, null);
		assertTrue(new File(targetFilename).exists());
	}
}
