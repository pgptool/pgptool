package org.pgptool.gui.hintsforusage.hints;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.tools.actions.LocalizedAction;

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
	@Resource(name = "keyRingService")
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

	private Action actionTellMeMore = new LocalizedAction("action.openFaq") {
		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				Desktop.getDesktop().browse(new URI("https://pgptool.github.io/#faq"));
			} catch (Throwable t) {
				log.error("Failed to pen browser", t);
				EntryPoint.reportExceptionToUser("failed.toOpenBrowser", t);
			}
		}
	};

	@Override
	protected Action[] getActions() {
		actionClose = new LocalizedAction("term.dismiss") {
			@Override
			public void actionPerformed(ActionEvent e) {
				hintsProps.put(CONFIG_SUPPRESS_HINT, true);
				if (hintHost != null) {
					hintHost.onClose();
				}
			}
		};

		Action actionCreate = new LocalizedAction("action.create") {
			@Override
			public void actionPerformed(ActionEvent e) {
				globalAppActions.getActionCreateKey().actionPerformed(e);
			}
		};

		Action actionImport = new LocalizedAction("action.import") {
			@Override
			public void actionPerformed(ActionEvent e) {
				globalAppActions.getActionImportKey().actionPerformed(e);
			}
		};

		return new Action[] { actionCreate, actionImport, actionTellMeMore, actionClose };
	};
}
