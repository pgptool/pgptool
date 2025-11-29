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
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import javax.swing.Action;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.hintsforusage.ui.HintPm;
import org.pgptool.gui.ui.tools.UrlOpener;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.springframework.beans.factory.InitializingBean;

public class BuyMeCoffeeHint extends HintPm implements InitializingBean {
  public static final String BUYMEACOFFEE_LINK = "https://www.buymeacoffee.com/skarpushind";
  private static final String CONFIG_SUPPRESS_HINT = "BuyMeCoffeeHint.supress";
  private static final String CONFIG_ELIGIBLE_SINCE = "BuyMeCoffeeHint.eligibleSince";
  private static final long LEAD_TIME = Duration.ofDays(14).toMillis();

  private final ConfigPairs hintsProps;
  private final ConfigPairs encryptionParams;
  private final ConfigPairs decryptionParams;
  private final HintsCoordinator hintsCoordinator;
  private final KeyRingService keyRingService;
  private final ExecutorService executorService;

  public BuyMeCoffeeHint(
      ConfigPairs hintsProps,
      ConfigPairs encryptionParams,
      ConfigPairs decryptionParams,
      HintsCoordinator hintsCoordinator,
      KeyRingService keyRingService,
      ExecutorService executorService) {
    this.hintsProps = hintsProps;
    this.encryptionParams = encryptionParams;
    this.decryptionParams = decryptionParams;
    this.hintsCoordinator = hintsCoordinator;
    this.keyRingService = keyRingService;
    this.executorService = executorService;
  }

  @Override
  public void afterPropertiesSet() {
    setMessage(Messages.text("hint.buyMeCoffee"));

    actionClose =
        new LocalizedActionEx("term.notThatHappy", this) {
          @Override
          public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            hintsProps.put(CONFIG_SUPPRESS_HINT, true);
            if (host != null) {
              host.onClose();
            }
          }
        };

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

    boolean hasKeys = !keyRingService.readKeys().isEmpty();
    boolean processedEnoughFiles =
        !encryptionParams.getAll().isEmpty() && !decryptionParams.getAll().isEmpty();
    if (!hasKeys || !processedEnoughFiles) {
      return false;
    }

    long eligibleSince = hintsProps.find(CONFIG_ELIGIBLE_SINCE, -1L);
    if (eligibleSince == -1) {
      hintsProps.put(CONFIG_ELIGIBLE_SINCE, System.currentTimeMillis());
      return false;
    }

    boolean isItTimeToShowThisHint = System.currentTimeMillis() - eligibleSince > LEAD_TIME;
    if (isItTimeToShowThisHint) {
      return true;
    }
    return false;
  }

  private final Action buyMeCoffeeAction =
      new LocalizedActionEx("action.buyMeCoffee", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          UrlOpener.open(e, BUYMEACOFFEE_LINK);
          actionClose.actionPerformed(e);
        }
      };

  @Override
  protected Action[] getActions() {
    return new Action[] {buyMeCoffeeAction, actionClose};
  }

  public Action getBuyMeCoffeeAction() {
    return buyMeCoffeeAction;
  }
}
