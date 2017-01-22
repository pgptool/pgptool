package org.pgpvault.gui.encryption.implpgp;

import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.pgpvault.gui.config.api.ConfigRepository;
import org.pgpvault.gui.encryption.api.KeyRingService;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;

public class KeyRingServicePgpImpl implements KeyRingService<KeyDataPgp> {
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
		}
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

}
