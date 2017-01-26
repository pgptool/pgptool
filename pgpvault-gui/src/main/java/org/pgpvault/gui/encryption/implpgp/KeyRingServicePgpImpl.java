package org.pgpvault.gui.encryption.implpgp;

import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class KeyRingServicePgpImpl implements KeyRingService<KeyDataPgp> {
	private static Logger log = Logger.getLogger(KeyRingServicePgpImpl.class);

	private ConfigRepository configRepository;
	private EventBus eventBus;

	private PgpKeysRing pgpKeysRing;

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public KeyRingServicePgpImpl() {
	}

	@Override
	public List<Key<KeyDataPgp>> readKeys() {
		ensureRead();
		// NOTE: Return copy of the list!
		return new ArrayList<>(pgpKeysRing);
	}

	private void ensureRead() {
		if (pgpKeysRing == null) {
			pgpKeysRing = configRepository.readOrConstruct(PgpKeysRing.class);
			dumpKeys();
		}
	}

	private void dumpKeys() {
		if (!log.isDebugEnabled()) {
			return;
		}

		for (Key<KeyDataPgp> key : pgpKeysRing) {
			log.debug("KEY --- " + key);
			if (key.getKeyData().getSecretKeyRing() != null) {
				PGPSecretKeyRing skr = key.getKeyData().getSecretKeyRing();

				log.debug("SECRET KEYRING: FIRST Public Key");
				logPublicKey(skr.getPublicKey());

				log.debug("SECRET KEYRING: ITERATING Public Keys");
				for (Iterator<PGPPublicKey> iterPK = skr.getPublicKeys(); iterPK.hasNext();) {
					PGPPublicKey pk = iterPK.next();
					logPublicKey(pk);
				}

				log.debug("SECRET KEYRING: FIRST Secret Key");
				logSecretKey(skr.getSecretKey());

				log.debug("SECRET KEYRING: ITERATING Secret Keys");
				for (Iterator<PGPSecretKey> iterPK = skr.getSecretKeys(); iterPK.hasNext();) {
					PGPSecretKey pk = iterPK.next();
					logSecretKey(pk);
				}
			} else {
				PGPPublicKeyRing pkr = key.getKeyData().getPublicKeyRing();

				log.debug("PUBLIC KEYRING: FIRST Public Key");
				logPublicKey(pkr.getPublicKey());

				log.debug("PUBLIC KEYRING: ITERATING Public Keys");
				for (Iterator<PGPPublicKey> iterPK = pkr.getPublicKeys(); iterPK.hasNext();) {
					PGPPublicKey pk = iterPK.next();
					logPublicKey(pk);
				}
			}
		}
	}

	private void logPublicKey(PGPPublicKey k) {
		String id = KeyDataPgp.buildKeyIdStr(k.getKeyID());
		String user = k.getUserIDs().hasNext() ? (String) k.getUserIDs().next() : "noUser";
		log.debug("... public key ID = " + id + ", isEncryption = " + k.isEncryptionKey() + ", isMaster = "
				+ k.isMasterKey() + ", user = " + user);
	}

	private void logSecretKey(PGPSecretKey k) {
		String id = KeyDataPgp.buildKeyIdStr(k.getKeyID());
		String user = k.getUserIDs().hasNext() ? (String) k.getUserIDs().next() : "noUser";
		log.debug("... secret key ID = " + id + ", isPrivateKeyEmpty = " + k.isPrivateKeyEmpty() + ", isSigningKey = "
				+ k.isSigningKey() + ", isMaster = " + k.isMasterKey() + ", user = " + user);
	}

	@Override
	public synchronized void addKey(Key<KeyDataPgp> key) {
		Preconditions.checkArgument(key != null, "key required");
		Preconditions.checkArgument(key.getKeyData() != null, "key data required");
		Preconditions.checkArgument(key.getKeyData() instanceof KeyDataPgp, "Wrong key data type");

		ensureRead();
		if (isKeyAlreadyAdded(key)) {
			throw new RuntimeException("This key was already added");
		}
		pgpKeysRing.add(key);
		configRepository.persist(pgpKeysRing);
		eventBus.post(EntityChangedEvent.added(key));
	}

	private boolean isKeyAlreadyAdded(Key<KeyDataPgp> newKey) {
		for (Key<KeyDataPgp> key : pgpKeysRing) {
			if (key.getKeyInfo().getKeyId().equals(newKey.getKeyInfo().getKeyId())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public synchronized void removeKey(Key<KeyDataPgp> key) {
		ensureRead();
		for (Iterator<Key<KeyDataPgp>> iter = pgpKeysRing.iterator(); iter.hasNext();) {
			Key<KeyDataPgp> cur = iter.next();
			if (cur.getKeyInfo().getKeyId().equals(key.getKeyInfo().getKeyId())) {
				iter.remove();
				configRepository.persist(pgpKeysRing);
				eventBus.post(EntityChangedEvent.removedObject(key));
				return;
			}
		}
	}

	public ConfigRepository getConfigRepository() {
		return configRepository;
	}

	@Autowired
	public void setConfigRepository(ConfigRepository configRepository) {
		this.configRepository = configRepository;
	}

	public EventBus getEventBus() {
		return eventBus;
	}

	@Autowired
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	@Override
	public List<Key<KeyDataPgp>> findMatchingDecryptionKeys(Set<String> keysIds) {
		Preconditions.checkArgument(!CollectionUtils.isEmpty(keysIds));

		List<Key<KeyDataPgp>> ret = new ArrayList<>(keysIds.size());
		List<Key<KeyDataPgp>> existingKeys = readKeys();
		for (String neededKeyId : keysIds) {
			for (Key<KeyDataPgp> existingKey : existingKeys) {
				if (!existingKey.getKeyData().isCanBeUsedForDecryption()) {
					existingKeys.remove(existingKey);
					break;
				}
				if (existingKey.getKeyData().isHasAlternativeId(neededKeyId)) {
					ret.add(existingKey);
					existingKeys.remove(existingKey);
					break;
				}
			}
		}
		return ret;
	}

}
