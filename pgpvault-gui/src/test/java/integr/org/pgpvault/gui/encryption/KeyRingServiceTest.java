package integr.org.pgpvault.gui.encryption;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.encryption.api.KeyFilesOperations;
import org.pgpvault.gui.encryption.api.KeyGeneratorService;
import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.CreateKeyParams;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;
import org.pgpvault.gui.encryption.implpgp.KeyRingServicePgpImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.annotation.SystemProfileValueSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.eventbus.EventBus;

import integr.org.pgpvault.gui.TestTools;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:integr-test-context.xml")
@ProfileValueSourceConfiguration(SystemProfileValueSource.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@SuppressWarnings({ "rawtypes", "unchecked" })
public class KeyRingServiceTest {
	@Autowired
	private KeyFilesOperations keyFilesOperations;
	@Autowired
	private KeyGeneratorService keyGeneratorService;

	@Autowired
	private ConfigRepository configRepository;
	@Autowired
	private EventBus eventBus;

	@Test
	public void testKeyRingServiceExpectCanFindKeyAfterSerialization() throws Exception {
		Key<KeyData> key = keyFilesOperations.readKeyFromFile(TestTools.getFileNameForResource("keys/Alice.asc"));

		KeyRingService<KeyData> keyRingService1 = buildAnotherKeyRingService();
		keyRingService1.addKey(key);
		List<Key<KeyData>> keys = keyRingService1.readKeys();
		assertNotNull(keys);
		assertEquals(1, keys.size());
		assertEquals("Alice <alice@email.com>", keys.get(0).getKeyInfo().getUser());

		KeyRingService<KeyData> keyRingService2 = buildAnotherKeyRingService();
		keys = keyRingService2.readKeys();
		assertNotNull(keys);
		assertEquals(1, keys.size());
		assertEquals("Alice <alice@email.com>", keys.get(0).getKeyInfo().getUser());
	}

	private KeyRingService<KeyData> buildAnotherKeyRingService() {
		KeyRingServicePgpImpl keyRingService1 = new KeyRingServicePgpImpl();
		keyRingService1.setConfigRepository(configRepository);
		keyRingService1.setEventBus(eventBus);
		keyRingService1.setKeyGeneratorService(Mockito.mock(KeyGeneratorService.class));
		return (KeyRingService) keyRingService1;
	}

	@Test
	public void testKeyCreation() throws Exception {
		Key key = keyGeneratorService.createNewKey(buildTestKey());
		assertNotNull(key);
	}

	static CreateKeyParams buildTestKey() {
		CreateKeyParams params = new CreateKeyParams();
		params.setFullName("Alpha Dog");
		params.setEmail("alpha.dog@email.com");
		params.setPassphrase("pass");
		params.setPassphraseAgain("pass");
		return params;
	}

	// Should we do rather something like this to reset context?
	// //
	// http://forum.spring.io/forum/spring-projects/container/56751-get-the-testcontext
	// private void refreshContext() {
	// testContext.markApplicationContextDirty(HierarchyMode.EXHAUSTIVE);
	// testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES_ATTRIBUTE,
	// Boolean.TRUE);
	// }
}
