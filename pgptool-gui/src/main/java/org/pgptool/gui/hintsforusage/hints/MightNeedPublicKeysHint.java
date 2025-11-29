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

import static org.summerb.utils.easycrud.api.dto.EntityChangedEvent.*;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import javax.swing.Action;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyTypeEnum;
import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.hintsforusage.ui.HintPm;
import org.pgptool.gui.ui.root.GlobalAppActions;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.springframework.beans.factory.InitializingBean;
import org.summerb.utils.easycrud.api.dto.EntityChangedEvent;

public class MightNeedPublicKeysHint extends HintPm implements InitializingBean {
  private static final String CONFIG_SUPPRESS_HINT = "MightNeedPublicKeysHint.supress";

  private final ConfigPairs hintsProps;
  private final HintsCoordinator hintsCoordinator;
  private final EventBus eventBus;
  private final KeyRingService keyRingService;
  private final GlobalAppActions globalAppActions;
  private final ExecutorService executorService;

  public MightNeedPublicKeysHint(
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
    setMessage(Messages.text("hint.needPublicKeys"));
    eventBus.register(this);
    executorService.execute(think);
  }

  private final Runnable think = this::doThink;

  private void doThink() {
    if (isHintApplicable()
        && !hintsCoordinator.isHintScheduled(CreateOrImportPrivateKeyHint.class)) {
      hintsCoordinator.scheduleHint(this);
    }
  }

  private boolean isHintApplicable() {
    if (hintsProps.find(CONFIG_SUPPRESS_HINT, false)) {
      return false;
    }

    int publicKeys = 0;
    int privateKeys = 0;

    for (Key key : keyRingService.readKeys()) {
      if (key.getKeyInfo().getKeyType() == KeyTypeEnum.KeyPair) {
        privateKeys++;
      } else {
        publicKeys++;
      }
    }
    return privateKeys > 0 && publicKeys == 0;
  }

  @Subscribe
  public void onKeyAdded(EntityChangedEvent<Key> e) {
    if (!e.isTypeOf(Key.class)) {
      return;
    }

    if (e.getChangeType() == ChangeType.ADDED
        && e.getValue().getKeyInfo().getKeyType() == KeyTypeEnum.Public) {
      hintsCoordinator.cancelHint(this);
    }
  }

  @Subscribe
  public void onHintsChanged(EntityChangedEvent<CreateOrImportPrivateKeyHint> e) {
    if (!e.isTypeOf(CreateOrImportPrivateKeyHint.class)) {
      return;
    }

    if (e.getChangeType() == ChangeType.ADDED) {
      hintsCoordinator.cancelHint(this);
    } else if (e.getChangeType() == ChangeType.REMOVED && isHintApplicable()) {
      hintsCoordinator.scheduleHint(this);
    }
  }

  private final Action actionTellMeMore =
      new LocalizedActionEx("action.openFaq", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          try {
            Desktop.getDesktop().browse(new URI("https://pgptool.github.io/#faq"));
          } catch (Throwable t) {
            EntryPoint.reportExceptionToUser(e, "failed.toOpenBrowser", t);
          }
        }
      };

  @Override
  protected Action[] getActions() {
    actionClose =
        new LocalizedActionEx("term.dismiss", this) {
          @Override
          public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            hintsProps.put(CONFIG_SUPPRESS_HINT, true);
            if (host != null) {
              host.onClose();
            }
          }
        };

    Action actionImport =
        new LocalizedActionEx("action.import", this) {
          @Override
          public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            globalAppActions.getActionImportKey().actionPerformed(e);
          }
        };

    return new Action[] {actionImport, actionTellMeMore, actionClose};
  }
  ;
}
