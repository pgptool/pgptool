package integr.org.pgpvault.gui.encryption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.ValidationException;

import org.apache.commons.io.FilenameUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pgpvault.gui.encryption.api.EncryptionService;
import org.pgpvault.gui.encryption.api.KeyFilesOperations;
import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.tools.TextFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.annotation.SystemProfileValueSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.summerb.approaches.security.api.exceptions.InvalidPasswordException;

import integr.org.pgpvault.gui.TestTools;

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

	private Key importKeyFromResources(String keyFileName) throws ValidationException, URISyntaxException {
		Key key = keyFilesOperations.readKeyFromFile(TestTools.getFileNameForResource("keys/" + keyFileName));
		keyRingService.addKey(key);
		keys.put(keyFileName, key);
		return key;
	}

	@Test
	public void testWeCanDecryptTheProductOfEncryption() throws Exception {
		String targetFilename = tempDirPath + File.separator + FilenameUtils.getBaseName(testSubjectFilename) + ".pgp";
		encryptionService.encrypt(testSubjectFilename, targetFilename, keys.values());
		encryptionService.decrypt(targetFilename, targetFilename + ".test", keys.get("Alice.asc"), "pass");
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

	private void decryptionTestRoutine(String sourceFile) throws InvalidPasswordException, URISyntaxException {
		String targetFilename = tempDirPath + File.separator + FilenameUtils.getBaseName(sourceFile);
		encryptionService.decrypt(TestTools.getFileNameForResource("encrypted/" + sourceFile), targetFilename,
				keys.get("Alice.asc"), "pass");
		assertTrue(new File(targetFilename).exists());
	}
}
