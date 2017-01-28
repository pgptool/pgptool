package org.pgpvault.gui.ui.keyslist;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.encryption.api.dto.Key;
import org.pgpvault.gui.encryption.api.dto.KeyData;
import org.pgpvault.gui.ui.tools.DialogViewBaseCustom;
import org.pgpvault.gui.ui.tools.UiUtils;

import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;

public class KeysListView extends DialogViewBaseCustom<KeysListPm> {
	private static final String DELETE = "Delete";

	private JPanel panelRoot;

	private JMenuBar menuBar;
	private JMenuItem miImport;
	private JMenuItem miDelete;
	private JMenuItem miClose;

	private JPanel panelTablePlaceholder;

	private JTable table;
	private JScrollPane scrollPane;
	private DefaultListSelectionModel selectionModel;
	private JPopupMenu ctxMenu;

	private JLabel lblNoDataToDisplay;

	@Override
	protected void internalInitComponents() {
		panelRoot = new JPanel(new BorderLayout());
		initMenuBar();
		initFormComponents();
		ctxMenu = new JPopupMenu();
	}

	private void initFormComponents() {
		panelTablePlaceholder = new JPanel(new BorderLayout());
		panelRoot.add(panelTablePlaceholder, BorderLayout.CENTER);
		panelTablePlaceholder.add(initTableComponent(), BorderLayout.CENTER);

		lblNoDataToDisplay = new JLabel(Messages.get("term.noDataToDisplay"));
		lblNoDataToDisplay.setHorizontalAlignment(JLabel.CENTER);
	}

	private JScrollPane initTableComponent() {
		table = new JTable();

		// Adjust some visual appearence
		table.setRowHeight(22);
		table.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

		// Add listeners
		selectionModel = new DefaultListSelectionModel();
		selectionModel.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);
		selectionModel.addListSelectionListener(rowSelectionListener);
		table.setSelectionModel(selectionModel);
		table.addMouseListener(listMouseListener);
		initTableKeyListener();

		// Envelope in scrollpane
		scrollPane = new JScrollPane();
		scrollPane.addMouseListener(listMouseListener);
		scrollPane.setViewportView(table);
		return scrollPane;
	}

	@SuppressWarnings("serial")
	private void initTableKeyListener() {
		int condition = JComponent.WHEN_IN_FOCUSED_WINDOW;
		InputMap inputMap = table.getInputMap(condition);
		ActionMap actionMap = table.getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), DELETE);
		actionMap.put(DELETE, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (pm == null) {
					return;
				}
				pm.getActionDelete().actionPerformed(e);
			}
		});
	}

	MouseAdapter listMouseListener = new MouseAdapter() {
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

		private void myPopupEvent(MouseEvent e) {
			if (!isAttached()) {
				return;
			}

			if (e.getComponent() == table) {
				int r = table.rowAtPoint(e.getPoint());
				if (r >= 0 && r < table.getRowCount() && pm.getTableModelProp().findRowByIdx(r) != null) {
					table.setRowSelectionInterval(r, r);
				} else {
					table.clearSelection();
				}
			} else {
				table.clearSelection();
			}

			if (ctxMenu.getComponentCount() > 0) {
				ctxMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (e.getComponent() == table && e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
				Action action = pm.getActionForRowDoubleClick();
				if (action != null && action.isEnabled()) {
					action.actionPerformed(null);
				}
			}
		}
	};

	protected ListSelectionListener rowSelectionListener = new ListSelectionListener() {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() || !isAttached()) {
				return;
			}

			Key<KeyData> selectedRow = getSelectedRow();
			if (selectedRow == null) {
				table.clearSelection();
			}
			pm.getSelectedRow().setValue(selectedRow);
		}
	};

	private Key<KeyData> getSelectedRow() {
		int row = table.getSelectedRow();
		if (row < 0) {
			return null;
		}
		return pm.getTableModelProp().findRowByIdx(row);
	}

	private TypedPropertyChangeListener<Key<KeyData>> rowPmSelectionListener = new TypedPropertyChangeListener<Key<KeyData>>() {
		@Override
		public void handlePropertyChanged(Object source, String propertyName, Key<KeyData> oldValue,
				Key<KeyData> newValue) {
			if (newValue == getSelectedRow()) {
				return;
			}

			if (newValue == null) {
				table.clearSelection();
				return;
			}

			int idx = pm.getTableModelProp().indexOf(newValue);
			if (idx < 0) {
				log.warn("Asked to select nonexistent record " + newValue + ". Skipping selection request.");
				return;
			}
			table.setRowSelectionInterval(idx, idx);
		}
	};

	private void initMenuBar() {
		menuBar = new JMenuBar();
		JMenu menuTs = new JMenu(Messages.get("term.actions"));
		menuTs.add(miImport = new JMenuItem());
		menuTs.addSeparator();
		menuTs.add(miDelete = new JMenuItem());
		menuTs.addSeparator();
		menuTs.add(miClose = new JMenuItem());
		menuBar.add(menuTs);
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		hasDataChangeHandler.handlePropertyChanged(this, pm.getHasData().getPropertyName(), false,
				pm.getHasData().getValue());
		bindingContext.registerOnChangeHandler(pm.getHasData(), hasDataChangeHandler);
		bindingContext.registerOnChangeHandler(pm.getSelectedRow(), rowPmSelectionListener);

		bindToActions();
	}

	private void bindToActions() {
		bindingContext.setupBinding(pm.getActionImport(), miImport);
		bindingContext.setupBinding(pm.getActionDelete(), miDelete);
		bindingContext.setupBinding(pm.getActionClose(), miClose);
		if (pm.getContextMenuActions() != null) {
			for (Action action : pm.getContextMenuActions()) {
				if (action == null) {
					ctxMenu.addSeparator();
				} else {
					ctxMenu.add(action);
				}
			}
		}
	}

	private TypedPropertyChangeListener<Boolean> hasDataChangeHandler = new TypedPropertyChangeListener<Boolean>() {
		@Override
		public void handlePropertyChanged(Object source, String propertyName, Boolean oldValue, Boolean newValue) {
			if (!newValue) {
				table.setModel(new DefaultTableModel());
				panelTablePlaceholder.removeAll();
				panelTablePlaceholder.add(lblNoDataToDisplay, BorderLayout.CENTER);
				panelTablePlaceholder.revalidate();
				panelTablePlaceholder.repaint();
				return;
			}

			table.setModel(pm.getTableModelProp());
			adjustColumnsWidths();
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
		ctxMenu.removeAll();
	}

	private void adjustColumn(DefaultTableCellRenderer leftRenderer, int columnIdx, int columnSize,
			DefaultTableCellRenderer cellRenderer) {
		table.getColumnModel().getColumn(columnIdx).setPreferredWidth(columnSize);
		table.getColumnModel().getColumn(columnIdx).setCellRenderer(cellRenderer);
	}

	private void adjustColumnsWidths() {
		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);

		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		rightRenderer.setHorizontalAlignment(JLabel.RIGHT);

		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(JLabel.CENTER);

		adjustColumn(leftRenderer, KeysTableModel.COLUMN_USER, spacing(30), leftRenderer);
		adjustColumn(leftRenderer, KeysTableModel.COLUMN_KEY_ID, spacing(16), centerRenderer);
		adjustColumn(leftRenderer, KeysTableModel.COLUMN_KEY_TYPE, spacing(8), centerRenderer);
		adjustColumn(leftRenderer, KeysTableModel.COLUMN_ALGORITHM, spacing(13), centerRenderer);
		adjustColumn(leftRenderer, KeysTableModel.COLUMN_CREATED_ON, spacing(10), centerRenderer);
		adjustColumn(leftRenderer, KeysTableModel.COLUMN_EXPIRES_AT, spacing(10), centerRenderer);
	}

	@Override
	protected JDialog initDialog(Window owner, Object constraints) {
		JDialog ret = new JDialog(owner, ModalityType.APPLICATION_MODAL);
		ret.setSize(new Dimension(spacing(75), spacing(30)));
		ret.setMinimumSize(new Dimension(spacing(50), spacing(25)));
		ret.setLayout(new BorderLayout());
		ret.setResizable(true);
		ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ret.setTitle(Messages.get("term.keysList"));
		ret.add(panelRoot, BorderLayout.CENTER);
		ret.setJMenuBar(menuBar);
		UiUtils.centerWindow(ret);
		return ret;
	}

	@Override
	protected JPanel getRootPanel() {
		return panelRoot;
	}

	@Override
	protected void dispatchWindowCloseEvent() {
		miClose.getAction().actionPerformed(null);
	}

}
