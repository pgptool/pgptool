package org.pgpvault.gui.ui.mainframe;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.pgpvault.gui.config.api.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;

public class MainFramePm extends PresentationModelBase {
	// private static Logger log = Logger.getLogger(MainFramePm.class);

	private ConfigRepository configRepository;

	private MainFrameHost host;

	public void init(MainFrameHost host) {
		Preconditions.checkArgument(host != null);
		Preconditions.checkState(this.host == null);

		this.host = host;
	}

	@SuppressWarnings("serial")
	private final Action actionConfigExit = new LocalizedAction("action.exit") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleExitApp();
		}
	};

	protected Action getActionConfigExit() {
		return actionConfigExit;
	}

	protected Action getActionAbout() {
		return host.getActionShowAboutInfo();
	}

	protected Action getActionImportKey() {
		return host.getActionImportKey();
	}

	protected Action getActionShowKeysList() {
		return host.getActionShowKeysList();
	}

	public Action getActionEncrypt() {
		return host.getActionForEncrypt();
	}

	public ConfigRepository getConfigRepository() {
		return configRepository;
	}

	@Autowired
	public void setConfigRepository(ConfigRepository configRepository) {
		this.configRepository = configRepository;
	}

}
