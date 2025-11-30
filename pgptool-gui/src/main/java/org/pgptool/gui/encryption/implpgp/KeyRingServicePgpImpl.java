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
package org.pgptool.gui.encryption.implpgp;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.MatchedKey;
import org.pgptool.gui.usage.api.UsageLogger;
import org.pgptool.gui.usage.dto.KeyAddedUsage;
import org.pgptool.gui.usage.dto.KeyRemovedUsage;
import org.pgptool.gui.usage.dto.KeyRingUsage;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent;

public class KeyRingServicePgpImpl implements KeyRingService {
  private static final Logger log = Logger.getLogger(KeyRingServicePgpImpl.class);

  private final ConfigRepository configRepository;
  private final EventBus eventBus;
  private final KeyGeneratorService keyGeneratorService;
  private final UsageLogger usageLogger;

  private PgpKeysRing pgpKeysRing;

  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  /** This method is created to ensure static constructor of this class was called */
  public static synchronized void touch() {}

  public KeyRingServicePgpImpl(
      ConfigRepository configRepository,
      EventBus eventBus,
      KeyGeneratorService keyGeneratorService,
      UsageLogger usageLogger) {
    this.configRepository = configRepository;
    this.eventBus = eventBus;
    this.keyGeneratorService = keyGeneratorService;
    this.usageLogger = usageLogger;
  }

  @Override
  public synchronized List<Key> readKeys() {
    ensureRead();
    // NOTE: Return copy of the list!
    return new ArrayList<>(pgpKeysRing);
  }

  private void ensureRead() {
    if (pgpKeysRing != null) {
      return;
    }

    synchronized (this) {
      if (pgpKeysRing != null) {
        return;
      }
      pgpKeysRing = configRepository.readOrConstruct(PgpKeysRing.class);

      // dumpKeys();
      if (pgpKeysRing.isEmpty()) {
        log.info(
            "User doesn't seem to have private key pair. Proactively generating one so that key creation will happen faster");
        keyGeneratorService.expectNewKeyCreation();
      }

      if (!pgpKeysRing.isEmpty()) {
        usageLogger.write(new KeyRingUsage(pgpKeysRing));
      }
    }
  }

  @Override
  public synchronized void addKey(Key key) {
    Preconditions.checkArgument(key != null, "key required");
    Preconditions.checkArgument(key.getKeyData() != null, "key data required");
    Preconditions.checkArgument(key.getKeyData() instanceof KeyDataPgp, "Wrong key data type");

    Key existingKey = findKeyById(key.getKeyInfo().getKeyId());
    if (existingKey != null) {
      if (!existingKey.getKeyData().isCanBeUsedForDecryption()
          && key.getKeyData().isCanBeUsedForDecryption()) {
        replaceKey(existingKey, key);
        return;
      } else {
        throw new RuntimeException("This key was already added");
      }
    }

    pgpKeysRing.add(key);
    configRepository.persist(pgpKeysRing);
    eventBus.post(EntityChangedEvent.added(key));
    usageLogger.write(new KeyAddedUsage(key.getKeyInfo().getKeyId()));
  }

  @Override
  public synchronized void replaceKey(Key key, Key newKey) {
    Preconditions.checkArgument(key != null, "key required");
    Preconditions.checkArgument(key.getKeyData() != null, "key data required");
    Preconditions.checkArgument(key.getKeyData() instanceof KeyDataPgp, "Wrong key data type");

    Preconditions.checkArgument(newKey != null, "newKey required");
    Preconditions.checkArgument(newKey.getKeyData() != null, "newKey data required");
    Preconditions.checkArgument(
        newKey.getKeyData() instanceof KeyDataPgp, "Wrong newKey data type");

    ensureRead();
    String keyId = key.getKeyInfo().getKeyId();
    for (Iterator<Key> iter = pgpKeysRing.iterator(); iter.hasNext(); ) {
      Key cur = iter.next();
      if (!cur.getKeyInfo().getKeyId().equals(keyId)) {
        continue;
      }

      iter.remove();
      pgpKeysRing.add(newKey);

      configRepository.persist(pgpKeysRing);
      eventBus.post(EntityChangedEvent.updated(newKey));
      return;
    }

    throw new IllegalStateException("Key " + keyId + " was not found in the ring");
  }

  @Override
  public synchronized Key findKeyById(String keyId) {
    Preconditions.checkArgument(StringUtils.hasText(keyId), "KeyId must be provided");
    ensureRead();

    for (Key cur : pgpKeysRing) {
      if (cur.getKeyInfo().getKeyId().equals(keyId) || cur.getKeyData().isHasAlternativeId(keyId)) {
        return cur;
      }
    }
    return null;
  }

  @Override
  public synchronized void removeKey(Key key) {
    ensureRead();
    for (Iterator<Key> iter = pgpKeysRing.iterator(); iter.hasNext(); ) {
      Key cur = iter.next();
      if (cur.getKeyInfo().getKeyId().equals(key.getKeyInfo().getKeyId())) {
        iter.remove();
        configRepository.persist(pgpKeysRing);
        eventBus.post(EntityChangedEvent.removedObject(key));
        usageLogger.write(new KeyRemovedUsage(key.getKeyInfo().getKeyId()));
        return;
      }
    }
  }

  /** keyIds passed here MIGHT NOT match key id from keyInfo */
  @Override
  public List<MatchedKey> findMatchingDecryptionKeys(Set<String> keysIds) {
    Preconditions.checkArgument(!CollectionUtils.isEmpty(keysIds));

    List<MatchedKey> ret = new ArrayList<>(keysIds.size());
    List<Key> allKeys = readKeys();
    List<Key> decryptionKeys =
        allKeys.stream().filter(x -> x.getKeyData().isCanBeUsedForDecryption()).toList();

    for (String neededKeyId : keysIds) {
      log.debug("Trying to find decryption key by id: " + neededKeyId);
      for (Key existingKey : decryptionKeys) {
        String user = existingKey.getKeyInfo().getUser();
        if (existingKey.getKeyData().isHasAlternativeId(neededKeyId)) {
          log.debug("Found matching key: " + user);
          ret.add(new MatchedKey(neededKeyId, existingKey));
          break;
        }
      }
    }
    return ret;
  }

  @Override
  public List<Key> findMatchingKeys(Set<String> keysIds) {
    Preconditions.checkArgument(!CollectionUtils.isEmpty(keysIds));

    List<Key> ret = new ArrayList<>(keysIds.size());
    List<Key> allKeys = readKeys();

    for (String neededKeyId : keysIds) {
      log.debug("Trying to find key by id: " + neededKeyId);
      for (Key existingKey : allKeys) {
        String user = existingKey.getKeyInfo().getUser();
        if (existingKey.getKeyData().isHasAlternativeId(neededKeyId)) {
          log.debug("Found matching key: " + user);
          ret.add(existingKey);
          break;
        }
      }
    }
    return ret;
  }
}
