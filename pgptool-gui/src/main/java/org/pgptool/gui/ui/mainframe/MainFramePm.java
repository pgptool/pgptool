package org.pgptool.gui.ui.mainframe;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.Action;

import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.config.api.ConfigRepository;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;
import org.pgptool.gui.decryptedlist.api.DecryptedHistoryService;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.ModelTableProperty;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class MainFramePm extends PresentationModelBase {
	// private static Logger log = Logger.getLogger(MainFramePm.class);

	private ConfigRepository configRepository;
	@Autowired
	private EventBus eventBus;
	@Autowired
	private DecryptedHistoryService decryptedHistoryService;

	private MainFrameHost host;

	private ModelTableProperty<DecryptedFile> rows;
	private ModelProperty<Boolean> hasData;

	public void init(MainFrameHost host) {
		Preconditions.checkArgument(host != null);
		Preconditions.checkState(this.host == null);

		this.host = host;

		List<DecryptedFile> initialKeys = decryptedHistoryService.getDecryptedFiles();
		rows = new ModelTableProperty<>(this, initialKeys, "decryptedFiles", new DecryptedFilesModel());
		hasData = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(!initialKeys.isEmpty()), "hasData");
		rows.getModelPropertyAccessor().addPropertyChangeListener(onSelectionChangedHandler);
		onSelectionChangedHandler.propertyChange(null);

		eventBus.register(this);
	}

	private PropertyChangeListener onSelectionChangedHandler = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			boolean hasSelection = rows.hasValue();

			for (int i = 0; i < contextMenuActions.length; i++) {
				Action action = contextMenuActions[i];
				if (action == null || !(action instanceof RowContextAction)) {
					continue;
				}
				((RowContextAction) action).setEnabled(hasSelection);
			}
		}
	};

	@Subscribe
	public void onRowChangedEvent(EntityChangedEvent<?> event) {
		if (!event.isTypeOf(DecryptedFile.class)) {
			return;
		}

		List<DecryptedFile> newKeysList = decryptedHistoryService.getDecryptedFiles();
		rows.getList().clear();
		rows.getList().addAll(newKeysList);
		hasData.setValueByOwner(!newKeysList.isEmpty());
		// NOTE: Selection is not nicely maintained. Each update will clear the
		// current selection if any
	}

	@Override
	public void detach() {
		super.detach();
		eventBus.unregister(this);
	}

	@SuppressWarnings("serial")
	protected Action actionOpen = new RowContextAction("action.openFile") {
		@Override
		public void onActionPerformed(DecryptedFile row) {
			try {
				Desktop.getDesktop().open(new File(row.getDecryptedFile()));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("error.cannotOpenFile", t);
			}
		}
	};

	@SuppressWarnings("serial")
	protected Action actionOpenSourceFolder = new RowContextAction("action.openSourceFolder") {
		@Override
		public void onActionPerformed(DecryptedFile row) {
			try {
				Desktop.getDesktop().open(new File(row.getEncryptedFile()).getParentFile());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("error.cannotOpenFolder", t);
			}
		}
	};
	@SuppressWarnings("serial")
	protected Action actionOpenTargetFolder = new RowContextAction("action.openTargetFolder") {
		@Override
		public void onActionPerformed(DecryptedFile row) {
			try {
				Desktop.getDesktop().open(new File(row.getDecryptedFile()).getParentFile());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("error.cannotOpenFolder", t);
			}
		}
	};

	@SuppressWarnings("serial")
	protected Action actionEncryptBack = new RowContextAction("action.encryptBack") {
		@Override
		public void onActionPerformed(DecryptedFile row) {
			host.openEncryptDialogFor(row.getDecryptedFile());
		}
	};

	@SuppressWarnings("serial")
	protected Action actionDelete = new RowContextAction("action.deleteUnencryptedFile") {
		@Override
		public void onActionPerformed(DecryptedFile row) {
			if (!UiUtils.confirm("confirmation.areUserSureToDeletDecryptedFile", null, findRegisteredWindowIfAny())) {
				return;
			}

			new File(row.getDecryptedFile()).delete();
			// NOTE: We're assuming here that historyService will detect file
			// was deleted and will fire an event saying that file is no longer
			// that so that corresponding row will disappear from table
		}
	};

	@SuppressWarnings("serial")
	protected Action actionForget = new RowContextAction("action.forgetDecrypted") {
		@Override
		public void onActionPerformed(DecryptedFile row) {
			decryptedHistoryService.remove(row.getDecryptedFile());
		}
	};

	protected Action[] contextMenuActions = new Action[] { actionOpen, actionOpenSourceFolder, actionOpenTargetFolder,
			null, actionEncryptBack, null, actionForget, actionDelete };

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
	
	protected Action getActionCreateKey() {
		return host.getActionCreateKey();
	}

	protected Action getActionShowKeysList() {
		return host.getActionShowKeysList();
	}

	public Action getActionEncrypt() {
		return host.getActionForEncrypt();
	}

	public Action getActionDecrypt() {
		return host.getActionForDecrypt();
	}

	public Action getActionChangeFolderForDecrypted() {
		return host.getActionChangeFolderForDecrypted();
	}

	public ModelTablePropertyAccessor<DecryptedFile> getRows() {
		return rows.getModelTablePropertyAccessor();
	}

	public ModelPropertyAccessor<DecryptedFile> getSelectedRow() {
		return rows.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<Boolean> getHasData() {
		return hasData.getModelPropertyAccessor();
	}

	public ConfigRepository getConfigRepository() {
		return configRepository;
	}

	@Autowired
	public void setConfigRepository(ConfigRepository configRepository) {
		this.configRepository = configRepository;
	}

	private abstract class RowContextAction extends LocalizedAction {
		private static final long serialVersionUID = -1541017110511442732L;

		public RowContextAction(String actionCode) {
			super(actionCode);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (!rows.hasValue()) {
				// silently exit -- not going to complain
				return;
			}
			DecryptedFile row = rows.getValue();
			onActionPerformed(row);
		}

		protected abstract void onActionPerformed(DecryptedFile row);
	}
}
