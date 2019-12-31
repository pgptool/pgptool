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
package org.pgptool.gui.hintsforusage.hints;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import javax.swing.Action;

import org.apache.commons.lang3.time.DateUtils;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.hintsforusage.ui.HintPm;
import org.pgptool.gui.ui.root.GlobalAppActions;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.easycrud.api.dto.EntityChangedEvent;
import org.summerb.easycrud.api.dto.EntityChangedEvent.ChangeType;
import org.summerb.utils.DtoBase;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.EXPORT.base.LocalizedActionEx;

@SuppressWarnings("serial")
public class PrivateKeyBackupHint extends HintPm implements InitializingBean {
	private static final String CONFIG_PREFIX = "PrivateKeyBackupReminderData:";

	@Autowired
	private ConfigPairs hintsProps;
	@Autowired
	private HintsCoordinator hintsCoordinator;
	@Autowired
	private EventBus eventBus;
	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private GlobalAppActions globalAppActions;
	@Autowired
	private ExecutorService executorService;

	private String lastHintForKeyId;
	private long reminderTriggerDelay = DateUtils.MILLIS_PER_DAY * 5;

	@Override
	public void afterPropertiesSet() throws Exception {
		setMessage(Messages.text("hint.privateKeyBackup", "TBD"));
		eventBus.register(this);
		executorService.execute(think);
	}

	private Runnable think = () -> {
		// See if there is a key we need to remind
		PrivateKeyBackupReminderData keyToBeExported = findKeyWhichbackIsDue();
		if (keyToBeExported == null) {
			return;
		}

		lastHintForKeyId = keyToBeExported.keyId;
		Key key = keyRingService.findKeyById(lastHintForKeyId);
		setMessage(Messages.text("hint.privateKeyBackup", key.getKeyInfo().getUser()));
		hintsCoordinator.scheduleHint(this);
	};

	private PrivateKeyBackupReminderData findKeyWhichbackIsDue() {
		List<PrivateKeyBackupReminderData> reminders = hintsProps.findAllWithPrefixedKey(CONFIG_PREFIX);
		if (reminders.size() == 0) {
			return null;
		}

		Set<String> presentKeys = keyRingService.readKeys().stream().map(x -> x.getKeyInfo().getKeyId())
				.collect(Collectors.toSet());
		if (presentKeys.size() == 0) {
			return null;
		}

		long triggerForCreatedBefore = System.currentTimeMillis() - reminderTriggerDelay;
		PrivateKeyBackupReminderData ret = reminders.stream()
				.filter(x -> x.timestamp <= triggerForCreatedBefore && presentKeys.contains(x.keyId)).findFirst()
				.orElse(null);
		return ret;
	}

	@Subscribe
	public void onKeyCreated(EntityChangedEvent<KeyCreatedEvent> e) {
		if (!e.isTypeOf(KeyCreatedEvent.class)) {
			return;
		}

		String keyId = e.getValue().key.getKeyInfo().getKeyId();
		hintsProps.put(CONFIG_PREFIX + keyId, new PrivateKeyBackupReminderData(keyId, System.currentTimeMillis()));
	}

	@Subscribe
	public void onKeyExported(EntityChangedEvent<PrivateKeyExportedEvent> e) {
		if (!e.isTypeOf(PrivateKeyExportedEvent.class)) {
			return;
		}

		String keyId = e.getValue().key.getKeyInfo().getKeyId();
		removeHintForKeyIfApplicable(keyId);
	}

	private void removeHintForKeyIfApplicable(String keyId) {
		hintsProps.put(CONFIG_PREFIX + keyId, null);

		if (lastHintForKeyId != null && keyId.equals(lastHintForKeyId)) {
			hintsCoordinator.cancelHint(this);
		}
	}

	@Subscribe
	public void onKeyDeleted(EntityChangedEvent<Key> e) {
		if (!e.isTypeOf(Key.class) || e.getChangeType() != ChangeType.REMOVED) {
			return;
		}

		removeHintForKeyIfApplicable(e.getValue().getKeyInfo().getKeyId());
	}

	@Override
	protected Action[] getActions() {
		actionClose = new LocalizedActionEx("term.dismiss", this) {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				removeHintForKeyIfApplicable(lastHintForKeyId);
			}
		};

		Action actionExport = new LocalizedActionEx("action.export", this) {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				Key key = keyRingService.findKeyById(lastHintForKeyId);
				globalAppActions.triggerPrivateKeyExport(key, e);
			}
		};

		return new Action[] { actionExport, actionClose };
	};

	public static class PrivateKeyBackupReminderData implements DtoBase {
		private String keyId;
		private long timestamp;

		public PrivateKeyBackupReminderData() {
		}

		public PrivateKeyBackupReminderData(String keyId, long timestamp) {
			super();
			this.keyId = keyId;
			this.timestamp = timestamp;
		}
	}

	public static class KeyCreatedEvent implements DtoBase {
		public Key key;

		public KeyCreatedEvent(Key key) {
			super();
			this.key = key;
		}
	}

	public static class PrivateKeyExportedEvent implements DtoBase {
		public Key key;

		public PrivateKeyExportedEvent(Key key) {
			super();
			this.key = key;
		}
	}
}
