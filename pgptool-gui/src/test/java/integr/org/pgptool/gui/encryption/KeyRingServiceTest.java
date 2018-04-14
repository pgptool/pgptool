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
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.pgptool.gui.encryption.implpgp.KeyRingServicePgpImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.annotation.SystemProfileValueSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.eventbus.EventBus;

import integr.org.pgptool.gui.TestTools;

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
