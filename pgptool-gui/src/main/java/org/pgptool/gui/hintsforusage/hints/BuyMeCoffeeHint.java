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

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.ExecutorService;

import javax.swing.Action;

import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.encryption.api.KeyRingService;
import org.pgptool.gui.hintsforusage.api.HintsCoordinator;
import org.pgptool.gui.hintsforusage.ui.HintPm;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

@SuppressWarnings("serial")
public class BuyMeCoffeeHint extends HintPm implements InitializingBean {
	public static final String BUYMEACOFFEE_LINK = "https://www.buymeacoffee.com/skarpushind";
	private static final String CONFIG_SUPPRESS_HINT = "BuyMeCoffeeHint.supress";
	private static final String CONFIG_ELIGIBLE_SINCE = "BuyMeCoffeeHint.eligibleSince";
	private static final long LEAD_TIME = Duration.ofDays(14).toMillis();

	@Autowired
	private ConfigPairs hintsProps;

	@Autowired
	private ConfigPairs encryptionParams;
	@Autowired
	private ConfigPairs decryptionParams;

	@Autowired
	private HintsCoordinator hintsCoordinator;
	@Autowired
	private KeyRingService keyRingService;
	@Autowired
	private ExecutorService executorService;

	@Override
	public void afterPropertiesSet() throws Exception {
		setMessage(Messages.text("hint.buyMeCoffee"));

		actionClose = new LocalizedActionEx("term.notThatHappy", this) {
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

	private Runnable think = () -> {
		if (isHintApplicable() && !hintsCoordinator.isHintScheduled(CreateOrImportPrivateKeyHint.class)) {
			hintsCoordinator.scheduleHint(this);
		}
	};

	private boolean isHintApplicable() {
		if (hintsProps.find(CONFIG_SUPPRESS_HINT, false)) {
			return false;
		}

		boolean hasKeys = keyRingService.readKeys().size() > 0;
		boolean processedEnoughFiles = encryptionParams.getAll().size() > 0 && decryptionParams.getAll().size() > 0;
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

	private Action buyMeCoffeeAction = new LocalizedActionEx("action.buyMeCoffee", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			try {
				Desktop.getDesktop().browse(new URI(BUYMEACOFFEE_LINK));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser(e, "failed.toOpenBrowser", t);
			}
			actionClose.actionPerformed(e);
		}
	};

	@Override
	protected Action[] getActions() {
		return new Action[] { buyMeCoffeeAction, actionClose };
	}

	public Action getBuyMeCoffeeAction() {
		return buyMeCoffeeAction;
	}

}
