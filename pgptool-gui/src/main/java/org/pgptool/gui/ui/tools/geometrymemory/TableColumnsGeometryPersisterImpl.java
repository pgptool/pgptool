package org.pgptool.gui.ui.tools.geometrymemory;

import java.util.ArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

public class TableColumnsGeometryPersisterImpl implements TableColumnModelListener, TableColumnsGeometryPersister {
	private static Logger log = Logger.getLogger(TableColumnsGeometryPersisterImpl.class);

	private static final long DELAY = 500;
	private static final TimeUnit DELAY_TIME_UNIT = TimeUnit.MILLISECONDS;

	private ConfigPairs configPairs;
	private ScheduledExecutorService scheduledExecutorService;
	private String keyId;
	private JTable table;

	private ArrayList<Pair<Integer, Integer>> prevColumnsConfig;
	private ScheduledFuture<?> persistFuture;

	/**
	 * @param scheduledExecutorService
	 *            it's used to dealy interraction with configPairs to avoid spamming
	 *            it with values while user still dragging element
	 */
	public TableColumnsGeometryPersisterImpl(JTable table, String keyId, ConfigPairs configPairs,
			ScheduledExecutorService scheduledExecutorService) {
		super();

		Preconditions.checkArgument(scheduledExecutorService != null);
		Preconditions.checkArgument(table.getColumnCount() > 0, "table should have initialized columns by now");
		Preconditions.checkArgument(StringUtils.hasText(keyId));
		Preconditions.checkArgument(configPairs != null);
		this.table = table;
		this.configPairs = configPairs;
		this.scheduledExecutorService = scheduledExecutorService;
		this.keyId = keyId + "_cols" + table.getColumnCount();
		table.getColumnModel().addColumnModelListener(this);
	}

	@Override
	public boolean restoreColumnsConfig() {
		Preconditions.checkState(table != null, "instance was detached, can't do much after that");
		ArrayList<Pair<Integer, Integer>> cc = configPairs.find(keyId, null);
		if (cc == null) {
			return false;
		}

		TableColumnModel cm = table.getColumnModel();

		for (int i = 0; i < cm.getColumnCount(); i++) {
			int desiredColumnModelIndex = cc.get(i).getLeft();
			if (cm.getColumn(i).getModelIndex() == desiredColumnModelIndex) {
				continue;
			}

			int desiredColumnPhysicalindex = getColumnPhysicalindexByModelIndex(cm, desiredColumnModelIndex);
			cm.moveColumn(desiredColumnPhysicalindex, i);
		}

		// xet sizes
		for (int i = 0; i < cm.getColumnCount(); i++) {
			TableColumn c = cm.getColumn(i);
			c.setPreferredWidth(cc.get(i).getRight());
		}

		prevColumnsConfig = cc;
		return true;
	}

	private int getColumnPhysicalindexByModelIndex(TableColumnModel cm, int desiredColumnModelIndex) {
		for (int j = 0; j < cm.getColumnCount(); j++) {
			if (cm.getColumn(j).getModelIndex() == desiredColumnModelIndex) {
				return j;
			}
		}
		throw new IllegalStateException("Column expected always to be found");
	}

	@Override
	public void columnMoved(TableColumnModelEvent e) {
		if (table == null) {
			return;
		}

		onColsConfigChanged();
	}

	@Override
	public void columnMarginChanged(ChangeEvent e) {
		if (table == null) {
			return;
		}

		onColsConfigChanged();
	}

	private void onColsConfigChanged() {
		if (!table.isVisible()) {
			return;
		}

		if (persistFuture != null) {
			persistFuture.cancel(true);
		}
		ArrayList<Pair<Integer, Integer>> newConfig = buildCurConfig();
		persistFuture = scheduledExecutorService.schedule(() -> doPersistColumnsConfig(newConfig), DELAY,
				DELAY_TIME_UNIT);
	}

	private void doPersistColumnsConfig(ArrayList<Pair<Integer, Integer>> columnsConfig) {
		if (columnsConfig.equals(prevColumnsConfig)) {
			return;
		}
		prevColumnsConfig = columnsConfig;
		configPairs.put(keyId, columnsConfig);
		log.debug(keyId + ": " + columnsConfig.toString());
	}

	private ArrayList<Pair<Integer, Integer>> buildCurConfig() {
		TableColumnModel cm = table.getColumnModel();
		ArrayList<Pair<Integer, Integer>> columnsConfig = new ArrayList<>(cm.getColumnCount());
		for (int i = 0; i < cm.getColumnCount(); i++) {
			TableColumn c = cm.getColumn(i);
			columnsConfig.add(Pair.of(c.getModelIndex(), c.getWidth()));
		}
		return columnsConfig;
	}

	@Override
	public boolean isAttached() {
		return table != null;
	}

	@Override
	public void detach() {
		Preconditions.checkState(table != null, "instance was detached, can't do much after that");
		table.getColumnModel().removeColumnModelListener(this);
		table = null;
	}

	@Override
	public void columnAdded(TableColumnModelEvent e) {
		// not handled
	}

	@Override
	public void columnRemoved(TableColumnModelEvent e) {
		// not handled
	}

	@Override
	public void columnSelectionChanged(ListSelectionEvent e) {
		// not handled
	}

}
