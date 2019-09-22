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

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.CompoundBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXLabel;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.decryptone.DecryptionDialogParameters;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.geometrymemory.TableColumnsGeometryPersister;
import org.pgptool.gui.ui.tools.geometrymemory.TableColumnsGeometryPersisterImpl;
import org.pgptool.gui.ui.tools.geometrymemory.WindowGeometryPersister;
import org.pgptool.gui.ui.tools.geometrymemory.WindowGeometryPersisterImpl;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.EXPORT.base.DialogViewBase;
import ru.skarpushin.swingpm.base.ViewBase;
import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class HistoryQuickSearchView extends ViewBase<HistoryQuickSearchPm> {
	@Autowired
	private ScheduledExecutorService scheduledExecutorService;
	@Autowired
	private ConfigPairs uiGeom;

	private JFrame window;
	private WindowGeometryPersister windowGeometryPersister;

	private JPanel panelRoot;
	private JPanel panelTablePlaceholder;
	private JLabel lblNoDataToDisplay;

	private JScrollPane scrollPane;
	private JTable table;
	protected TableColumnsGeometryPersister tableColumnsGeometryPersister;
	private DefaultListSelectionModel selectionModel;

	private JPopupMenu ctxMenu;

	private JLabel tblLabel;
	private JTextField edQuickSearch;
	private JButton btnCancel;

	@Override
	protected void internalInitComponents() {
		SgLayout sgl = new SgLayout(2, 5, 0, 0);
		sgl.setColSize(0, 100, SgLayout.SIZE_TYPE_WEIGHTED);
		sgl.setColSize(1, UiUtils.getFontRelativeSize(text("action.cancel").length()), SgLayout.SIZE_TYPE_ASKCOMPONENT);
		sgl.setRowSize(3, UiUtils.getFontRelativeSize(26), SgLayout.SIZE_TYPE_WEIGHTED);
		panelRoot = new JPanel(sgl);

		CompoundBorder panelBorder = BorderFactory.createCompoundBorder(BorderFactory.createRaisedBevelBorder(),
				BorderFactory.createEmptyBorder(5, 5, 5, 5));
		panelRoot.setBorder(panelBorder);

		panelRoot.add(new JLabel(text("phrase.typeFilenameToFilter")), sgl.cs(0, 0, 2, 1));
		edQuickSearch = new JTextField();
		edQuickSearch.addKeyListener(quickSearchKeyListener);
		JPanel pnlEditQuickSearch = new JPanel(new BorderLayout());
		pnlEditQuickSearch.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
		pnlEditQuickSearch.add(edQuickSearch, BorderLayout.CENTER);
		panelRoot.add(pnlEditQuickSearch, sgl.cs(0, 1, 2, 1));

		tblLabel = new JLabel("TBD");
		panelRoot.add(tblLabel, sgl.cs(0, 2, 2, 1));

		panelTablePlaceholder = new JPanel(new BorderLayout());
		panelRoot.add(panelTablePlaceholder, sgl.cs(0, 3, 2, 1));

		JXLabel hintLbl = new JXLabel(text("quicksearch.navigationHint"));
		hintLbl.setLineWrap(true);
		panelRoot.add(hintLbl, sgl.cs(0, 4));

		JPanel tmpPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		panelRoot.add(tmpPanel, sgl.cs(1, 4));
		tmpPanel.add(btnCancel = new JButton());

		ctxMenu = new JPopupMenu();
		initTableComponent();

		lblNoDataToDisplay = new JLabel(text("term.noDataToDisplay"));
		lblNoDataToDisplay.setHorizontalAlignment(JLabel.CENTER);
	}

	private KeyListener quickSearchKeyListener = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
			int rowCount = table.getRowCount();
			if (rowCount == 0) {
				return;
			}

			if (e.getKeyCode() == KeyEvent.VK_DOWN) {
				int row = table.getSelectedRow();
				if (row < 0) {
					e.consume();
					table.setRowSelectionInterval(0, 0);
				} else if (row < rowCount - 1) {
					e.consume();
					table.setRowSelectionInterval(row + 1, row + 1);
				}
			} else if (e.getKeyCode() == KeyEvent.VK_UP) {
				int row = table.getSelectedRow();
				if (row < 0) {
					e.consume();
					table.setRowSelectionInterval(0, 0);
				} else if (row > 0) {
					e.consume();
					table.setRowSelectionInterval(row - 1, row - 1);
				}
			} else if (e.getKeyCode() == KeyEvent.VK_ENTER && table.getSelectedRow() >= 0) {
				e.consume();
				pm.actionOpen.actionPerformed(null);
			} else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
				pm.getActionCancel().actionPerformed(null);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action enterAction = new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent e) {
			pm.actionOpen.actionPerformed(e);
		}
	};

	private JScrollPane initTableComponent() {
		table = new JTable();

		// Adjust some visual appearance
		table.setRowHeight(22);
		table.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		// Add listeners
		selectionModel = new DefaultListSelectionModel();
		selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener((evt) -> pm.setSelected(getSelectedRow()));
		table.setSelectionModel(selectionModel);
		table.addMouseListener(listMouseListener);

		table.getActionMap().put("confirmRow", enterAction);
		table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "confirmRow");

		// Envelope in scrollpane
		scrollPane = new JScrollPane();
		scrollPane.setViewportView(table);
		return scrollPane;
	}

	private MouseAdapter listMouseListener = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			super.mouseClicked(e);
			if (!isAttached()) {
				return;
			}

			if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
				pm.actionOpen.actionPerformed(null);
				return;
			}
		}

		private void myPopupEvent(MouseEvent e) {
			updateTableSelection(e);
			if (ctxMenu.getComponentCount() > 0) {
				ctxMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		private void updateTableSelection(MouseEvent e) {
			if (e.getComponent() == table) {
				int r = table.rowAtPoint(e.getPoint());
				if (r >= 0 && r < table.getRowCount()) {
					table.setRowSelectionInterval(r, r);
				} else {
					table.clearSelection();
				}
			} else {
				table.clearSelection();
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			if (e.isPopupTrigger()) {
				myPopupEvent(e);
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			if (e.isPopupTrigger()) {
				myPopupEvent(e);
			}
		}
	};

	private DecryptionDialogParameters getSelectedRow() {
		int row = table.getSelectedRow();
		if (row < 0) {
			return null;
		}

		// NOTE: This is a internal convention that by columnIndex -1 we mean
		// object itself
		TableModel tableModel = pm.getRowsTableModel().getValue();
		if (tableModel == null) {
			return null;
		}
		return (DecryptionDialogParameters) tableModel.getValueAt(row, -1);
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		tableModelChangeHandler.handlePropertyChanged(this, pm.getRowsTableModel().getPropertyName(), null,
				pm.getRowsTableModel().getValue());
		bindingContext.registerOnChangeHandler(pm.getRowsTableModel(), tableModelChangeHandler);

		bindingContext.setupBinding(pm.getTableLabel(), tblLabel);
		bindingContext.setupBinding(pm.getQuickSearch(), edQuickSearch);

		bindingContext.setupBinding(pm.getActionCancel(), btnCancel);

		bindContextMenu();
	}

	private void bindContextMenu() {
		if (pm.contextMenuActions != null) {
			for (Action action : pm.contextMenuActions) {
				if (action == null) {
					ctxMenu.addSeparator();
				} else {
					ctxMenu.add(action);
				}
			}
		}
	}

	private TypedPropertyChangeListener<TableModel> tableModelChangeHandler = new TypedPropertyChangeListener<TableModel>() {
		@Override
		public void handlePropertyChanged(Object source, String propertyName, TableModel oldValue,
				TableModel newValue) {
			if (newValue == null || newValue.getRowCount() == 0) {
				table.setModel(new DefaultTableModel());
				panelTablePlaceholder.removeAll();
				panelTablePlaceholder.add(lblNoDataToDisplay, BorderLayout.CENTER);
				panelTablePlaceholder.revalidate();
				panelTablePlaceholder.repaint();
				return;
			}

			if (tableColumnsGeometryPersister != null) {
				tableColumnsGeometryPersister.detach();
				tableColumnsGeometryPersister = null;
			}
			table.setModel(newValue);
			table.setRowSelectionInterval(0, 0);
			tableColumnsGeometryPersister = new TableColumnsGeometryPersisterImpl(table, "tblHstQSrch", uiGeom,
					scheduledExecutorService);
			tableColumnsGeometryPersister.restoreColumnsConfig();

			table.repaint();
			panelTablePlaceholder.removeAll();
			panelTablePlaceholder.add(scrollPane, BorderLayout.CENTER);
			panelTablePlaceholder.revalidate();
			panelTablePlaceholder.repaint();
		}
	};

	@Override
	protected void internalUnbindFromPm() {
		super.internalUnbindFromPm();
		table.setModel(new DefaultTableModel());
		table.repaint();
		ctxMenu.removeAll();
	}

	@Override
	protected void internalRenderTo(Container owner, Object constraints) {
		Preconditions.checkArgument(owner == null);
		// Preconditions.checkArgument(constraints != null && constraints instanceof
		// Rectangle);
		Preconditions.checkState(pm != null, "PM is required for this view");

		if (window == null) {
			window = new JFrame(text("term.quickSearch"));
			window.setLayout(new BorderLayout());
			window.add(panelRoot, BorderLayout.CENTER);
			window.setResizable(true);
			window.setMinimumSize(new Dimension(UiUtils.getFontRelativeSize(50), UiUtils.getFontRelativeSize(25)));

			windowGeometryPersister = new WindowGeometryPersisterImpl(window, "histQSrch", uiGeom,
					scheduledExecutorService);
			if (!windowGeometryPersister.restoreSize()) {
				window.pack();
			}

			window.addComponentListener(componentAdapter);
			window.addWindowListener(windowAdapter);
			DialogViewBase.installEscapeCloseOperation(window, window.getRootPane());
		}

		Point currentMousePosition = MouseInfo.getPointerInfo().getLocation();
		window.setLocation(findBestWindowLocation(currentMousePosition));

		window.setVisible(true);
		window.setAlwaysOnTop(true);
	}

	private WindowAdapter windowAdapter = new WindowAdapter() {
		@Override
		public void windowDeactivated(java.awt.event.WindowEvent e) {
			if (!isAttached()) {
				return;
			}

			pm.getActionCancel().actionPerformed(null);
		};
	};

	private ComponentListener componentAdapter = new ComponentAdapter() {
		@Override
		public void componentHidden(ComponentEvent e) {
		}

		@Override
		public void componentShown(ComponentEvent e) {
			edQuickSearch.requestFocusInWindow();
		}
	};

	private Point findBestWindowLocation(Point pt) {
		Rectangle screenBounds = findGraphicsDeviceByPoint(pt);
		if (screenBounds == null) {
			return pt;
		}

		final int w = window.getWidth();
		final int h = window.getHeight();
		if (screenBounds.width < w || screenBounds.height < h) {
			return pt;
		}
		int x = pt.x;
		int y = pt.y;

		int xboundary = screenBounds.x + screenBounds.width;
		if (x + w > xboundary) {
			x = pt.x - (x + w - xboundary);
		}

		int yboundary = screenBounds.y + screenBounds.height;
		if (y + h > yboundary) {
			y = pt.y - (y + h - yboundary);
		}

		return new Point(x, y);
	}

	public static Rectangle findGraphicsDeviceByPoint(Point location) {
		// Check if the location is in the bounds of one of the graphics
		// devices.
		GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] graphicsDevices = graphicsEnvironment.getScreenDevices();
		Rectangle bounds = new Rectangle();

		// Iterate over the graphics devices.
		for (int j = 0; j < graphicsDevices.length; j++) {
			// Get the bounds of the device.
			GraphicsDevice graphicsDevice = graphicsDevices[j];
			bounds.setRect(graphicsDevice.getDefaultConfiguration().getBounds());

			// Is the location in this bounds?
			bounds.setRect(bounds.x, bounds.y, bounds.width, bounds.height);
			if (bounds.contains(location.x, location.y)) {
				// The location is in this screengraphics.
				return bounds;
			}
		}

		// We could not find a device that contains the given point.
		return null;
	}

	@Override
	protected void internalUnrender() {
		if (window != null) {
			window.setVisible(false);
		}
	}

}
