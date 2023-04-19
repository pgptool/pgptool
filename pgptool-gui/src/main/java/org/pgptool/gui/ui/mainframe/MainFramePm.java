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
package org.pgptool.gui.ui.mainframe;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.apache.commons.io.FilenameUtils;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;
import org.pgptool.gui.decryptedlist.api.MonitoringDecryptedFilesService;
import org.pgptool.gui.hintsforusage.api.HintsHolder;
import org.pgptool.gui.hintsforusage.ui.HintPm;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgptool.gui.ui.checkForUpdates.UpdatesPolicy;
import org.pgptool.gui.ui.decryptone.DecryptionDialogParameters;
import org.pgptool.gui.ui.historyquicksearch.HistoryQuickSearchHost;
import org.pgptool.gui.ui.historyquicksearch.HistoryQuickSearchPm;
import org.pgptool.gui.ui.historyquicksearch.HistoryQuickSearchView;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.summerb.easycrud.api.dto.EntityChangedEvent;
import org.summerb.easycrud.api.dto.EntityChangedEvent.ChangeType;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.modelprops.table.ModelTableProperty;
import ru.skarpushin.swingpm.modelprops.table.ModelTablePropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class MainFramePm extends PresentationModelBaseEx<MainFrameHost, UpdatesPolicy>
		implements ApplicationContextAware, HintsHolder {
	// private static Logger log = Logger.getLogger(MainFramePm.class);

	@Autowired
	private EventBus eventBus;
	@Autowired
	private MonitoringDecryptedFilesService monitoringDecryptedFilesService;
	@Autowired
	private DecryptedTempFolder decryptedTempFolder;

	private HistoryQuickSearchView historyQuickSearchView;
	private HistoryQuickSearchPm historyQuickSearchPm;

	private ModelTableProperty<DecryptedFile> rows;
	private ModelProperty<Boolean> hasData;

	private ModelProperty<HintPm> hint = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "hint");

	@Override
	public boolean init(ActionEvent originAction, MainFrameHost host, UpdatesPolicy updatesPolicy) {
		Preconditions.checkState(this.host == null);
		Preconditions.checkArgument(host != null);
		super.init(originAction, host, updatesPolicy);

		historyQuickSearchPm.init(originAction, historyQuickSearchHost, null);

		List<DecryptedFile> initialKeys = monitoringDecryptedFilesService.getDecryptedFiles();
		rows = new ModelTableProperty<>(this, initialKeys, "decryptedFiles",
				new DecryptedFilesModel(decryptedTempFolder));
		hasData = new ModelProperty<Boolean>(this, new ValueAdapterHolderImpl<>(!initialKeys.isEmpty()), "hasData") {
			@Override
			public boolean setValueByOwner(Boolean value) {
				actionEncryptBackAll.setEnabled(value);
				return super.setValueByOwner(value);
			}
		};
		actionEncryptBackAll.setEnabled(hasData.getValue());
		rows.getModelPropertyAccessor().addPropertyChangeListener(onSelectionChangedHandler);
		onSelectionChangedHandler.propertyChange(null);

		eventBus.register(this);
		return true;
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

		@SuppressWarnings("unchecked")
		EntityChangedEvent<DecryptedFile> e = (EntityChangedEvent<DecryptedFile>) event;

		ListEx<DecryptedFile> list = rows.getList();
		if (e.getChangeType() == ChangeType.ADDED) {
			list.add(e.getValue());
		} else if (e.getChangeType() == ChangeType.UPDATED) {
			int index = list.indexOf(e.getValue());
			if (index < 0) {
				list.add(e.getValue());
			} else {
				list.set(index, e.getValue());
			}
		} else {
			int prevSelIndex = list.indexOf(event.getValue());
			list.remove(prevSelIndex);
			if (!list.isEmpty()) {
				rows.setValueByOwner(list.get(Math.min(list.size() - 1, prevSelIndex)));
			}
		}

		hasData.setValueByOwner(!list.isEmpty());
	}

	@Override
	public void detach() {
		super.detach();
		eventBus.unregister(this);

		if (historyQuickSearchView != null) {
			historyQuickSearchView.unrender();
			historyQuickSearchView.detach();
			historyQuickSearchView = null;
		}

		historyQuickSearchPm.detach();
	}

	@SuppressWarnings("serial")
	protected Action actionOpen = new RowContextAction("action.openFile") {
		@Override
		public void onActionPerformed(DecryptedFile row, ActionEvent e) {
			try {
				Desktop.getDesktop().open(new File(row.getDecryptedFile()));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser(e, "error.cannotOpenFile", t);
			}
		}
	};

	@SuppressWarnings("serial")
	protected Action actionOpenSourceFolder = new RowContextAction("action.openSourceFolder") {
		@Override
		public void onActionPerformed(DecryptedFile row, ActionEvent e) {
			try {
				Desktop.getDesktop().open(new File(row.getEncryptedFile()).getParentFile());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser(e, "error.cannotOpenFolder", t);
			}
		}
	};
	@SuppressWarnings("serial")
	protected Action actionOpenTargetFolder = new RowContextAction("action.openTargetFolder") {
		@Override
		public void onActionPerformed(DecryptedFile row, ActionEvent e) {
			try {
				Desktop.getDesktop().open(new File(row.getDecryptedFile()).getParentFile());
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser(e, "error.cannotOpenFolder", t);
			}
		}
	};

	@SuppressWarnings("serial")
	protected Action actionEncryptBack = new RowContextAction("action.encryptBack") {
		@Override
		public void onActionPerformed(DecryptedFile row, ActionEvent e) {
			host.openEncryptDialogFor(row.getDecryptedFile(), e);
		}
	};

	@SuppressWarnings("serial")
	protected Action actionDelete = new RowContextAction("action.deleteUnencryptedFile") {
		@Override
		public void onActionPerformed(DecryptedFile row, ActionEvent e) {
			if (!new File(row.getEncryptedFile()).exists()) {
				if (!UiUtils.confirmWarning(e, "confirmation.areUserSureToDeletDecryptedFileWhileSourceIsNotFound",
						new Object[] { FilenameUtils.getName(row.getDecryptedFile()), row.getEncryptedFile() })) {
					return;
				}
			} else {
				if (!UiUtils.confirmRegular(e, "confirmation.areUserSureToDeletDecryptedFile",
						new Object[] { FilenameUtils.getName(row.getDecryptedFile()) })) {
					return;
				}
			}

			File file = new File(row.getDecryptedFile());
			// NOTE: We're assuming here that historyService will detect file
			// was deleted and will fire an event saying that file is no longer
			// that so that corresponding row will disappear from table
			if (!file.exists() || file.delete()) {
				return;
			}

			UiUtils.messageBox(e, Messages.get("error.cannotDeletebecauseFileIsLocked"), Messages.get("term.error"),
					JOptionPane.ERROR_MESSAGE);
		}
	};

	@SuppressWarnings("serial")
	protected Action actionForget = new RowContextAction("action.forgetDecrypted") {
		@Override
		public void onActionPerformed(DecryptedFile row, ActionEvent e) {
			monitoringDecryptedFilesService.remove(row.getDecryptedFile());
		}
	};

	protected Action[] contextMenuActions = new Action[] { actionOpen, actionOpenSourceFolder, actionOpenTargetFolder,
			null, actionEncryptBack, null, actionForget, actionDelete };

	@SuppressWarnings("serial")
	private final Action actionConfigExit = new LocalizedActionEx("action.exit", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			host.handleExitApp();
		}
	};

	@SuppressWarnings("serial")
	protected Action actionEncryptBackAll = new LocalizedActionEx("encrypBackMany.encryptBackAll", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			ArrayList<DecryptedFile> files = new ArrayList<>(rows.getList());
			Preconditions.checkState(files.size() > 0,
					"This action supposed to be available only when ther is at least one decrypted file monitored");

			Set<String> fileNames = files.stream().map(x -> x.getDecryptedFile()).collect(Collectors.toSet());
			host.openEncryptBackMultipleFor(fileNames, e);
		}
	};
	private ApplicationContext applicationContext;

	protected Action getActionConfigExit() {
		return actionConfigExit;
	}

	protected Action getActionAbout() {
		return host.getActionShowAboutInfo();
	}

	public Action getActionBuyMeCoffee() {
		return host.getActionBuyMeCoffee();
	}

	public Action getActionAskQuestionInChat() {
		return host.getAskQuestionInChat();
	}

	public Action getActionReportIssue() {
		return host.getActionReportIssue();
	}

	public Action getActionFaq() {
		return host.getActionFaq();
	}

	public Action getActionHelp() {
		return host.getActionHelp();
	}

	public Action getActionCheckForUpdates() {
		return host.getActionCheckForUpdates();
	}

	protected Action getActionImportKey() {
		return host.getActionImportKey();
	}

	protected Action getActionImportKeyFromText() {
		return host.getActionImportKeyFromText();
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

	public Action getActionEncryptText() {
		return host.getActionForEncryptText();
	}

	public Action getActionDecryptText() {
		return host.getActionForDecryptText();
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

	private abstract class RowContextAction extends LocalizedActionEx {
		private static final long serialVersionUID = -1541017110511442732L;

		public RowContextAction(String actionCode) {
			super(actionCode, MainFramePm.this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			if (!rows.hasValue()) {
				// silently exit -- not going to complain
				return;
			}
			DecryptedFile row = rows.getValue();
			onActionPerformed(row, e);
		}

		protected abstract void onActionPerformed(DecryptedFile row, ActionEvent e);
	}

	public Action getActionAutoCheckForUpdates() {
		return initParams.actionAutoCheckForUpdates;
	}

	public ModelPropertyAccessor<?> getIsAutoUpdatesEnabled() {
		return initParams.getIsAutoUpdatesEnabled();
	}

	private HistoryQuickSearchHost historyQuickSearchHost = new HistoryQuickSearchHost() {
		@Override
		public void handleChosen(DecryptionDialogParameters optionalTsRecordSubject, ActionEvent originEvent) {
			getHistoryQuickSearchView().unrender();
			if (optionalTsRecordSubject == null) {
				return;
			}
			// NOTE: We override it here to avoid wrong behavior -- quick search is already
			// closed so as a parent for this action it is useless
			ActionEvent overiddenActionEvent = UiUtils.actionEvent(findRegisteredWindowIfAny(),
					originEvent.getActionCommand());
			host.openDecryptDialogFor(optionalTsRecordSubject.getSourceFile(), overiddenActionEvent);
		}

		@Override
		public void handleCancel() {
			getHistoryQuickSearchView().unrender();
		}
	};

	@SuppressWarnings("serial")
	protected Action actionHistoryQuickSearch = new LocalizedActionEx("term.history", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			getHistoryQuickSearchPm().refreshRecentlyUsed(); // this was added to address #168. Doesn't feel like the
																// best approach, but I cannot find any better at this
																// moment. One of the options would be to monitor all
																// files using FileWatcher and refresh this list in case
																// of any changes, but that seem to be overwhelming a
																// bit. Assuming source files will not disappear all the
																// time, background check is still more preferable,
																// even if once in a blue moon UI will refresh after
																// already been rendered
			getHistoryQuickSearchView().renderTo(null);
		}
	};

	private HistoryQuickSearchView getHistoryQuickSearchView() {
		if (historyQuickSearchView == null) {
			historyQuickSearchView = applicationContext.getBean(HistoryQuickSearchView.class);
			historyQuickSearchView.setPm(getHistoryQuickSearchPm());
		}
		return historyQuickSearchView;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public HistoryQuickSearchPm getHistoryQuickSearchPm() {
		return historyQuickSearchPm;
	}

	@Autowired
	public void setHistoryQuickSearchPm(HistoryQuickSearchPm historyQuickSearchPm) {
		this.historyQuickSearchPm = historyQuickSearchPm;
	}

	public ModelPropertyAccessor<HintPm> getHintPm() {
		return hint.getModelPropertyAccessor();
	}

	@Override
	public HintPm getHint() {
		return hint.getValue();
	}

	@Override
	public void setHint(HintPm hint) {
		this.hint.setValueByOwner(hint);
	}
}
