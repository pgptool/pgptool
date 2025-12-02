/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
package integr.org.pgptool.gui.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.common.eventbus.EventBus;
import integr.org.pgptool.gui.TestTools;
import integr.org.pgptool.gui.config.IntegrTestConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.implpgp.KeyRingServicePgpImpl;
import org.pgptool.gui.usage.api.UsageLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.ProfileValueSourceConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {IntegrTestConfig.class})
@ProfileValueSourceConfiguration()
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
public class KeyRingServiceTest {
  @Autowired KeyFilesOperations keyFilesOperations;
  @Autowired KeyGeneratorService keyGeneratorService;
  @Autowired ConfigRepository configRepository;
  @Autowired EventBus eventBus;
  @Autowired UsageLogger usageLogger;

  @Test
  public void testKeyRingServiceExpectCanFindKeyAfterSerialization() throws Exception {
    Key key =
        keyFilesOperations.readKeyFromFile(TestTools.getFileNameForResource("keys/Alice.asc"));

    KeyRingService keyRingService1 = buildAnotherKeyRingService();
    keyRingService1.addKey(key);
    List<Key> keys = keyRingService1.readKeys();
    assertNotNull(keys);
    assertEquals(1, keys.size());
    assertEquals("Alice <alice@email.com>", keys.get(0).getKeyInfo().getUser());

    KeyRingService keyRingService2 = buildAnotherKeyRingService();
    keys = keyRingService2.readKeys();
    assertNotNull(keys);
    assertEquals(1, keys.size());
    assertEquals("Alice <alice@email.com>", keys.get(0).getKeyInfo().getUser());
  }

  private KeyRingService buildAnotherKeyRingService() {
    return new KeyRingServicePgpImpl(configRepository, eventBus, usageLogger);
  }

  @Test
  public void testKeyCreation() throws Exception {
    Key key = keyGeneratorService.createNewKey(buildTestKey(), false);
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
