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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
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
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.springframework.beans.factory.InitializingBean;
import org.summerb.utils.DtoBase;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent.ChangeType;

public class PrivateKeyBackupHint extends HintPm implements InitializingBean {
  private static final String CONFIG_PREFIX = "PrivateKeyBackupReminderData:";

  private final ConfigPairs hintsProps;
  private final HintsCoordinator hintsCoordinator;
  private final EventBus eventBus;
  private final KeyRingService keyRingService;
  private final GlobalAppActions globalAppActions;
  private final ExecutorService executorService;

  private String lastHintForKeyId;
  private final long reminderTriggerDelay = DateUtils.MILLIS_PER_DAY * 5;

  public PrivateKeyBackupHint(
      ConfigPairs hintsProps,
      HintsCoordinator hintsCoordinator,
      EventBus eventBus,
      KeyRingService keyRingService,
      GlobalAppActions globalAppActions,
      ExecutorService executorService) {
    this.hintsProps = hintsProps;
    this.hintsCoordinator = hintsCoordinator;
    this.eventBus = eventBus;
    this.keyRingService = keyRingService;
    this.globalAppActions = globalAppActions;
    this.executorService = executorService;
  }

  @Override
  public void afterPropertiesSet() {
    setMessage(Messages.text("hint.privateKeyBackup", "TBD"));
    eventBus.register(this);
    executorService.execute(think);
  }

  private final Runnable think = this::doThink;

  private void doThink() {
    // See if there is a key we need to remind
    PrivateKeyBackupReminderData keyToBeExported = findKeyWhichBackupIsDue();
    if (keyToBeExported == null) {
      return;
    }

    lastHintForKeyId = keyToBeExported.keyId;
    Key key = keyRingService.findKeyById(lastHintForKeyId);
    setMessage(Messages.text("hint.privateKeyBackup", key.getKeyInfo().getUser()));
    hintsCoordinator.scheduleHint(this);
  }

  private PrivateKeyBackupReminderData findKeyWhichBackupIsDue() {
    List<PrivateKeyBackupReminderData> reminders = hintsProps.findAllWithPrefixedKey(CONFIG_PREFIX);
    if (reminders.isEmpty()) {
      return null;
    }

    Set<String> presentKeys =
        keyRingService.readKeys().stream()
            .map(x -> x.getKeyInfo().getKeyId())
            .collect(Collectors.toSet());
    if (presentKeys.isEmpty()) {
      return null;
    }

    long triggerForCreatedBefore = System.currentTimeMillis() - reminderTriggerDelay;
    return reminders.stream()
        .filter(x -> x.timestamp <= triggerForCreatedBefore && presentKeys.contains(x.keyId))
        .findFirst()
        .orElse(null);
  }

  @Subscribe
  public void onKeyCreated(EntityChangedEvent<KeyCreatedEvent> e) {
    if (!e.isTypeOf(KeyCreatedEvent.class)) {
      return;
    }

    String keyId = e.getValue().key.getKeyInfo().getKeyId();
    hintsProps.put(
        CONFIG_PREFIX + keyId, new PrivateKeyBackupReminderData(keyId, System.currentTimeMillis()));
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

    //noinspection PointlessNullCheck
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
    actionClose =
        new LocalizedActionEx("term.dismiss", this) {
          @Override
          public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            removeHintForKeyIfApplicable(lastHintForKeyId);
          }
        };

    Action actionExport =
        new LocalizedActionEx("action.export", this) {
          @Override
          public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            Key key = keyRingService.findKeyById(lastHintForKeyId);
            globalAppActions.triggerPrivateKeyExport(key, e);
          }
        };

    return new Action[] {actionExport, actionClose};
  }

  public static class PrivateKeyBackupReminderData implements DtoBase {
    private String keyId;
    private long timestamp;

    public PrivateKeyBackupReminderData() {}

    public PrivateKeyBackupReminderData(String keyId, long timestamp) {
      super();
      this.keyId = keyId;
      this.timestamp = timestamp;
    }
  }

  public static class KeyCreatedEvent implements DtoBase {
    public final Key key;

    public KeyCreatedEvent(Key key) {
      super();
      this.key = key;
    }
  }

  public static class PrivateKeyExportedEvent implements DtoBase {
    public final Key key;

    public PrivateKeyExportedEvent(Key key) {
      super();
      this.key = key;
    }
  }
}
