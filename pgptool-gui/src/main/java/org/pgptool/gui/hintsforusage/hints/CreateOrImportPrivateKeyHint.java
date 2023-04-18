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
import java.util.concurrent.ExecutorService;

import javax.swing.Action;

import org.apache.log4j.Logger;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.easycrud.api.dto.EntityChangedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class CreateOrImportPrivateKeyHint extends HintPm implements InitializingBean {
	private static Logger log = Logger.getLogger(CreateOrImportPrivateKeyHint.class);
	private static final String CONFIG_SUPPRESS_HINT = "CreateOrImportPrivateKeyHint.supress";

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

	@Override
	public void afterPropertiesSet() throws Exception {
		setMessage(Messages.text("hint.needPrivateKey"));
		eventBus.register(this);
		executorService.execute(think);
	}

	private Runnable think = () -> {
		long privateKeysCount = keyRingService.readKeys().stream()
				.filter(x -> x.getKeyInfo().getKeyType() == KeyTypeEnum.KeyPair).count();
		if (privateKeysCount == 0 && hintsProps.find(CONFIG_SUPPRESS_HINT, false) != true) {
			hintsCoordinator.scheduleHint(this);
		}
	};

	@Subscribe
	public void onKeyAdded(EntityChangedEvent<Key> e) {
		if (!e.isTypeOf(Key.class)) {
			return;
		}

		if (e.getValue().getKeyInfo().getKeyType() == KeyTypeEnum.KeyPair) {
			hintsCoordinator.cancelHint(this);
		}
	}

	private Action actionTellMeMore = new LocalizedActionEx("action.openFaq", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			try {
				Desktop.getDesktop().browse(new URI("https://pgptool.github.io/#faq"));
			} catch (Throwable t) {
				log.error("Failed to open browser", t);
				EntryPoint.reportExceptionToUser(e, "failed.toOpenBrowser", t);
			}
		}
	};

	@Override
	protected Action[] getActions() {
		actionClose = new LocalizedActionEx("term.dismiss", this) {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				hintsProps.put(CONFIG_SUPPRESS_HINT, true);
				if (host != null) {
					host.onClose();
				}
			}
		};

		Action actionCreate = new LocalizedActionEx("action.create", this) {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				globalAppActions.getActionCreateKey().actionPerformed(e);
			}
		};

		Action actionImport = new LocalizedActionEx("action.import", this) {
			@Override
			public void actionPerformed(ActionEvent e) {
				super.actionPerformed(e);
				globalAppActions.getActionImportKey().actionPerformed(e);
			}
		};

		return new Action[] { actionCreate, actionImport, actionTellMeMore, actionClose };
	};
}
