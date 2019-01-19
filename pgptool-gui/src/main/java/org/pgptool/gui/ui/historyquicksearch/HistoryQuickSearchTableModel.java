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
package org.pgptool.gui.ui.historyquicksearch;

import java.util.List;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.apache.commons.io.FilenameUtils;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.ui.decryptone.DecryptionDialogParameters;

import com.google.common.base.Preconditions;

public class HistoryQuickSearchTableModel implements TableModel {
	public static final int COLUMN_NAME = 0;
	public static final int COLUMN_PATH = 1;

	private final List<DecryptionDialogParameters> decryptions;

	public HistoryQuickSearchTableModel(List<DecryptionDialogParameters> decryptions) {
		this.decryptions = decryptions;
	}

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
		case COLUMN_NAME:
			return Messages.get("term.name");
		case COLUMN_PATH:
			return Messages.get("term.path");
		default:
			throw new IllegalArgumentException("Wrong column index: " + columnIndex);
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		DecryptionDialogParameters s = decryptions.get(rowIndex);

		switch (columnIndex) {
		case -1:
			return s;
		case COLUMN_NAME:
			return " " + FilenameUtils.getName(s.getSourceFile());
		case COLUMN_PATH:
			return " " + FilenameUtils.getFullPath(s.getSourceFile());
		default:
			throw new IllegalArgumentException("Wrong column index: " + columnIndex);
		}
	}

	@Override
	public int getRowCount() {
		return decryptions.size();
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		Preconditions.checkState(false, "Operation not supported");
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// no events here
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		// no events here
	}

}
