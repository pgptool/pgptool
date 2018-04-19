package org.pgptool.gui.ui.historyquicksearch;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.swing.Action;
import javax.swing.table.TableModel;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.decryptone.DecryptionDialogParameters;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

public class HistoryQuickSearchPm extends PresentationModelBase implements InitializingBean {
	private static Logger log = Logger.getLogger(HistoryQuickSearchPm.class);

	private EventBus eventBus;
	private ExecutorService executorService;
	@Autowired
	private ConfigPairs decryptionParams;

	@SuppressWarnings("rawtypes")
	private Future refreshRecentlyUsedBkgFuture;
	private Future<TableModel> quickSearchResultsFuture;

	private ModelProperty<TableModel> rowsTableModel;
	private ModelProperty<String> tableLabel;

	private TableModel lastPopularRecords;

	private HistoryQuickSearchHost host;

	private boolean listenerRegistered;

	public void init(HistoryQuickSearchHost host) {
		Preconditions.checkArgument(host != null);
		this.host = host;

		rowsTableModel = new ModelProperty<TableModel>(this, new ValueAdapterHolderImpl<TableModel>(),
				"rowsTableModel");

		tableLabel = new ModelProperty<String>(this,
				new ValueAdapterHolderImpl<String>(Messages.get("term.recentlyDecrypted")), "tableLabel");

		handleRecordChanged(null);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		eventBus.register(this);
		listenerRegistered = true;
	}

	private ModelProperty<String> quickSearch = new ModelProperty<String>(this, new ValueAdapterHolderImpl<String>(""),
			"quickSearch") {
		@Override
		public boolean setValueByOwner(String value) {
			if (!super.setValueByOwner(value)) {
				return false;
			}

			cancelQuickSearchIfAny();

			if ("".equals(value)) {
				tableLabel.setValueByOwner(Messages.get("term.recentlyDecrypted"));
				rowsTableModel.setValueByOwner(lastPopularRecords);
			} else {
				// Remove table
				rowsTableModel.setValueByOwner(null);
				// Run bkg thread for search
				quickSearchResultsFuture = executorService.submit(quickSearchBkgWorker);
			}

			return true;
		}
	};

	protected void cancelQuickSearchIfAny() {
		if (quickSearchResultsFuture != null && !quickSearchResultsFuture.isDone()) {
			quickSearchResultsFuture.cancel(true);
			quickSearchResultsFuture = null;
		}
	}

	private Callable<TableModel> quickSearchBkgWorker = new Callable<TableModel>() {
		@Override
		public TableModel call() {
			try {
				String query = quickSearch.getValue();
				log.debug("Quick search for record subject: " + query);
				tableLabel.setValueByOwner(Messages.get("term.searching", query));

				Set<Entry<String, Object>> all = decryptionParams.getAll();
				List<DecryptionDialogParameters> results = all.stream()
						.map(x -> (DecryptionDialogParameters) x.getValue())
						/*
						 * TODO: Some weird thing happening here, records are getting duplicated, would
						 * be great to understand WHY ???. In a mean time we'll just de-duplicate them.
						 */
						.filter(distinctByKey(x -> x.getSourceFile()))
						.filter(x -> x.getSourceFile().toLowerCase().contains(query.toLowerCase()))
						.sorted(byTimestampDesc).map(x -> Pair.of(x, new File(x.getSourceFile())))
						.filter(x -> x.getRight().isFile() && x.getRight().exists()).map(x -> x.getLeft())
						.collect(Collectors.toList());

				HistoryQuickSearchTableModel tableModel = CollectionUtils.isEmpty(results) ? null
						: new HistoryQuickSearchTableModel(results);
				if (isQuickSearchMode()) {
					if (!query.equals(quickSearch.getValue())) {
						return null;
					}
					rowsTableModel.setValueByOwner(tableModel);
				}

				return tableModel;
			} catch (Throwable t) {
				log.error("Failed to refresh search records subjects", t);
				// EntryPoint.reportExceptionToUser("exception.unexpected", t);
				return null;
			} finally {
				if (!isQuickSearchMode()) {
					return null;
				}
				tableLabel.setValueByOwner(Messages.get("term.searchResults"));
			}
		}
	};

	@Subscribe
	public void handleRecordChanged(EntityChangedEvent<?> evt) {
		try {
			if (evt != null && !evt.isTypeOf(DecryptionDialogParameters.class)) {
				return;
			}

			if (refreshRecentlyUsedBkgFuture != null && !refreshRecentlyUsedBkgFuture.isDone()) {
				// it's not critical to have most up-to-date data. If there is a
				// work being done - do not interrupt it
				return;
			}

			refreshRecentlyUsedBkgFuture = executorService.submit(refreshSubjectsBkgWorker);
		} catch (Throwable exc) {
			log.error("Failed to handle EntityChangedEvent and schedule refreshSubjectsBkgWorker", exc);
		}
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}

	private Callable<TableModel> refreshSubjectsBkgWorker = new Callable<TableModel>() {
		@Override
		public TableModel call() {
			try {
				log.debug("Refresh for most popular records subject");

				List<DecryptionDialogParameters> topRecords = decryptionParams.getAll().stream()
						.map(x -> (DecryptionDialogParameters) x.getValue())
						.filter(distinctByKey(x -> x.getSourceFile())).sorted(byTimestampDesc)
						.map(x -> Pair.of(x, new File(x.getSourceFile())))
						.filter(x -> x.getRight().isFile() && x.getRight().exists()).map(x -> x.getLeft()).limit(15)
						.collect(Collectors.toList());

				lastPopularRecords = CollectionUtils.isEmpty(topRecords) ? null
						: new HistoryQuickSearchTableModel(topRecords);
				if (!isQuickSearchMode()) {
					rowsTableModel.setValueByOwner(lastPopularRecords);
				}
				return lastPopularRecords;
			} catch (Throwable t) {
				log.error("Failed to refresh records subjects", t);
				return null;
			}
		}
	};

	protected Comparator<DecryptionDialogParameters> byTimestampDesc = new Comparator<DecryptionDialogParameters>() {
		@Override
		public int compare(DecryptionDialogParameters o1, DecryptionDialogParameters o2) {
			if (o2.getCreatedAt() < o1.getCreatedAt()) {
				return -1;
			}
			if (o2.getCreatedAt() > o1.getCreatedAt()) {
				return 1;
			}
			return 0;
		}
	};

	protected boolean isQuickSearchMode() {
		return !"".equals(quickSearch.getValue());
	}

	@Override
	public boolean isAttached() {
		return super.isAttached() || listenerRegistered;
	}

	@Override
	public void detach() {
		super.detach();
		eventBus.unregister(this);
		listenerRegistered = false;

		if (refreshRecentlyUsedBkgFuture != null && !refreshRecentlyUsedBkgFuture.isDone()) {
			refreshRecentlyUsedBkgFuture.cancel(true);
			refreshRecentlyUsedBkgFuture = null;
		}

		if (quickSearchResultsFuture != null && !quickSearchResultsFuture.isDone()) {
			quickSearchResultsFuture.cancel(true);
			quickSearchResultsFuture = null;
		}
	}

	protected void handleDoubleClick(DecryptionDialogParameters row) {
		host.handleChosen(row);
		quickSearch.setValueByOwner("");
	}

	@SuppressWarnings("serial")
	private final Action actionCancel = new LocalizedAction("action.cancel") {
		@Override
		public void actionPerformed(ActionEvent e) {
			host.handleCancel();
			quickSearch.setValueByOwner("");
		}
	};

	public EventBus getEventBus() {
		return eventBus;
	}

	@Autowired
	public void setEventBus(EventBus eventBus) {
		this.eventBus = eventBus;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	@Autowired
	public void setExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	protected ModelPropertyAccessor<TableModel> getRowsTableModel() {
		return rowsTableModel.getModelPropertyAccessor();
	}

	protected ModelPropertyAccessor<String> getTableLabel() {
		return tableLabel.getModelPropertyAccessor();
	}

	protected ModelPropertyAccessor<String> getQuickSearch() {
		return quickSearch.getModelPropertyAccessor();
	}

	protected Action getActionCancel() {
		return actionCancel;
	}
}
