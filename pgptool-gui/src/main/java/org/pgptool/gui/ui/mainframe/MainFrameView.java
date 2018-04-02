/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
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
 *******************************************************************************/
package org.pgptool.gui.ui.mainframe;

import static org.pgptool.gui.app.Messages.text;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
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

import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.WindowIcon;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.HasWindow;
import ru.skarpushin.swingpm.base.ViewBase;
import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;
import ru.skarpushin.swingpm.tools.SwingPmSettings;
import ru.skarpushin.swingpm.tools.sglayout.SgLayout;

public class MainFrameView extends ViewBase<MainFramePm> implements HasWindow {
	private static final String DELETE = "Delete";

	private JFrame frame;

	private JPanel panelRoot;

	private JMenuBar menuBar;
	private JMenuItem miPgpCreateKey;
	private JMenuItem miPgpImportKey;
	private JMenuItem miShowKeyList;
	private JMenuItem miChangeTempFolderForDecrypted;
	private JMenuItem miBmc;
	private JMenuItem miFaq;
	private JMenuItem miHelp;
	private JMenuItem miAbout;
	private JMenuItem miCheckForUpdates;
	private JCheckBoxMenuItem miAutoCheckForUpdates;
	private JMenuItem miWriteFeedback;
	private JMenuItem miConfigExit;
	private JMenuItem miEncrypt;
	private JMenuItem miDecrypt;
	private JMenuItem miEncryptText;
	private JMenuItem miDecryptText;
	private JMenuItem miEncryptBackAll;

	private JPanel panelTablePlaceholder;
	private JTable table;
	private JScrollPane scrollPane;
	private DefaultListSelectionModel selectionModel;
	private JPopupMenu ctxMenu;
	private JLabel lblNoDataToDisplay;

	@Override
	protected void internalInitComponents() {
		initMenuBar();
		initFormComponents();

		ctxMenu = new JPopupMenu();
	}

	private void initFormComponents() {
		lblNoDataToDisplay = new JLabel(Messages.get("phrase.noDecryptedFilesAreMonitoredAtTheMoment"));
		lblNoDataToDisplay.setHorizontalAlignment(JLabel.CENTER);

		SgLayout sgl = new SgLayout(1, 4, 0, 0);
		sgl.setColSize(0, 100, SgLayout.SIZE_TYPE_WEIGHTED);
		sgl.setRowSize(3, 100, SgLayout.SIZE_TYPE_WEIGHTED);
		panelRoot = new JPanel(sgl);
		panelRoot.setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));

		int row = 0;
		panelRoot.add(new JLabel(UiUtils.plainToBoldHtmlString(text("phrase.primaryOperations"))), sgl.cs(0, row));

		row++;
		JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		pnlButtons.add(new JButton(actionEncrypt));
		pnlButtons.add(new JLabel("  "));
		pnlButtons.add(new JButton(actionEncryptText));
		pnlButtons.add(new JLabel("  "));
		pnlButtons.add(new JButton(actionDecrypt));
		pnlButtons.add(new JLabel("  "));
		pnlButtons.add(new JButton(actionDecryptText));
		pnlButtons.add(new JLabel("  "));
		pnlButtons.add(new JButton(actionKeyring));
		panelRoot.add(pnlButtons, sgl.cs(0, row));

		row++;
		JLabel lblPrevDecryptedFiles = new JLabel(
				UiUtils.plainToBoldHtmlString(text("phrase.previouslyDecrpytedFiles")));
		panelRoot.add(lblPrevDecryptedFiles, sgl.cs(0, row));
		lblPrevDecryptedFiles.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));

		row++;
		panelTablePlaceholder = new JPanel(new BorderLayout());
		panelTablePlaceholder.add(initTableComponent(), BorderLayout.CENTER);
		panelRoot.add(panelTablePlaceholder, sgl.cs(0, row));
	}

	@SuppressWarnings("serial")
	private Action actionEncrypt = new ToolbarAction("action.encryptFile", "/icons/encrypt.png") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (pm != null) {
				pm.getActionEncrypt().actionPerformed(e);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action actionEncryptText = new ToolbarAction("action.encryptText", "/icons/encrypt.png") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (pm != null) {
				pm.getActionEncryptText().actionPerformed(e);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action actionDecryptText = new ToolbarAction("action.decryptText", "/icons/decrypt.png") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (pm != null) {
				pm.getActionDecryptText().actionPerformed(e);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action actionDecrypt = new ToolbarAction("action.decryptFile", "/icons/decrypt.png") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (pm != null) {
				pm.getActionDecrypt().actionPerformed(e);
			}
		}
	};

	@SuppressWarnings("serial")
	private Action actionKeyring = new ToolbarAction("term.keyring", "/icons/keyring.png") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (pm != null) {
				pm.getActionShowKeysList().actionPerformed(e);
			}
		}
	};

	private JScrollPane initTableComponent() {
		table = new JTable();

		// Adjust some visual appearence
		table.setRowHeight(22);

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
				pm.actionDelete.actionPerformed(e);
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
				if (r >= 0 && r < table.getRowCount() && pm.getRows().findRowByIdx(r) != null) {
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
				Action action = pm.actionOpen;
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

			DecryptedFile selectedRow = getSelectedRow();
			if (selectedRow == null) {
				table.clearSelection();
			}
			pm.getSelectedRow().setValue(selectedRow);
		}
	};

	private DecryptedFile getSelectedRow() {
		int row = table.getSelectedRow();
		if (row < 0) {
			return null;
		}
		return pm.getRows().findRowByIdx(row);
	}

	private TypedPropertyChangeListener<DecryptedFile> rowPmSelectionListener = new TypedPropertyChangeListener<DecryptedFile>() {
		@Override
		public void handlePropertyChanged(Object source, String propertyName, DecryptedFile oldValue,
				DecryptedFile newValue) {
			if (newValue == getSelectedRow()) {
				return;
			}

			if (newValue == null) {
				table.clearSelection();
				return;
			}

			int idx = pm.getRows().indexOf(newValue);
			if (idx < 0) {
				log.warn("Asked to select nonexistent record " + newValue + ". Skipping selection request.");
				return;
			}
			table.setRowSelectionInterval(idx, idx);
		}
	};

	private void initMenuBar() {
		menuBar = new JMenuBar();

		JMenu menuFile = new JMenu(Messages.get("term.appTitle"));
		menuFile.add(miChangeTempFolderForDecrypted = new JMenuItem());
		menuFile.addSeparator();
		menuFile.add(miBmc = new JMenuItem());
		menuFile.add(miWriteFeedback = new JMenuItem());
		menuFile.add(miFaq = new JMenuItem());
		menuFile.add(miHelp = new JMenuItem());
		menuFile.addSeparator();
		menuFile.add(miAbout = new JMenuItem());
		menuFile.add(miCheckForUpdates = new JMenuItem());
		menuFile.add(miAutoCheckForUpdates = new JCheckBoxMenuItem());
		menuFile.addSeparator();
		menuFile.add(miConfigExit = new JMenuItem());

		JMenu menuKeyring = new JMenu(Messages.get("term.keyring"));
		menuKeyring.add(miShowKeyList = new JMenuItem());
		menuKeyring.addSeparator();
		menuKeyring.add(miPgpImportKey = new JMenuItem());
		menuKeyring.add(miPgpCreateKey = new JMenuItem());

		JMenu menuActions = new JMenu(Messages.get("term.actions"));
		menuActions.add(miEncrypt = new JMenuItem());
		menuActions.add(miDecrypt = new JMenuItem());
		menuActions.addSeparator();
		menuActions.add(miEncryptText = new JMenuItem());
		menuActions.add(miDecryptText = new JMenuItem());
		menuActions.addSeparator();
		menuActions.add(miEncryptBackAll = new JMenuItem());

		menuBar.add(menuFile);
		menuBar.add(menuKeyring);
		menuBar.add(menuActions);
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		updateWindowTitle();
		bindToActions();
		bindTable();
		bindContextMenu();
	}

	private void bindTable() {
		hasDataChangeHandler.handlePropertyChanged(this, pm.getHasData().getPropertyName(), false,
				pm.getHasData().getValue());
		bindingContext.registerOnChangeHandler(pm.getHasData(), hasDataChangeHandler);
		bindingContext.registerOnChangeHandler(pm.getSelectedRow(), rowPmSelectionListener);
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

			table.setModel(pm.getRows());
			adjustColumnsWidths();
			table.repaint();
			panelTablePlaceholder.removeAll();
			panelTablePlaceholder.add(scrollPane, BorderLayout.CENTER);
			panelTablePlaceholder.revalidate();
			panelTablePlaceholder.repaint();
		}
	};

	private void bindToActions() {
		bindingContext.setupBinding(pm.getActionConfigExit(), miConfigExit);

		bindingContext.setupBinding(pm.getActionBuyMeCoffee(), miBmc);
		bindingContext.setupBinding(pm.getActionFaq(), miFaq);
		bindingContext.setupBinding(pm.getActionHelp(), miHelp);

		bindingContext.setupBinding(pm.getActionAbout(), miAbout);
		bindingContext.setupBinding(pm.getActionCheckForUpdates(), miCheckForUpdates);
		bindingContext.setupBinding(pm.getActionAutoCheckForUpdates(), miAutoCheckForUpdates);
		bindingContext.setupBinding(pm.getActionShowFeedbackForm(), miWriteFeedback);
		bindingContext.registerPropertyValuePropagation(pm.getIsAutoUpdatesEnabled(), miAutoCheckForUpdates, "state");

		bindingContext.setupBinding(pm.getActionImportKey(), miPgpImportKey);
		bindingContext.setupBinding(pm.getActionCreateKey(), miPgpCreateKey);
		bindingContext.setupBinding(pm.getActionShowKeysList(), miShowKeyList);
		bindingContext.setupBinding(pm.getActionChangeFolderForDecrypted(), miChangeTempFolderForDecrypted);
		bindingContext.setupBinding(pm.getActionEncrypt(), miEncrypt);
		bindingContext.setupBinding(pm.getActionEncryptText(), miEncryptText);
		bindingContext.setupBinding(pm.getActionDecryptText(), miDecryptText);
		bindingContext.setupBinding(pm.actionEncryptBackAll, miEncryptBackAll);
		bindingContext.setupBinding(pm.getActionDecrypt(), miDecrypt);
	}

	private void updateWindowTitle() {
		if (frame != null) {
			frame.setTitle(Messages.get("term.appTitle"));
		}
	}

	@Override
	protected void internalUnbindFromPm() {
		super.internalUnbindFromPm();

		updateWindowTitle();

		table.setModel(new DefaultTableModel());
		ctxMenu.removeAll();
	}

	private void adjustColumnsWidths() {
		DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
		leftRenderer.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		leftRenderer.setHorizontalAlignment(JLabel.LEFT);
		adjustColumn(leftRenderer, DecryptedFilesModel.COLUMN_ENCRYPTED_FILE, 50, leftRenderer);
		adjustColumn(leftRenderer, DecryptedFilesModel.COLUMN_DECRYPTED_FILE, 50, leftRenderer);
	}

	private void adjustColumn(DefaultTableCellRenderer leftRenderer, int columnIdx, int columnSize,
			DefaultTableCellRenderer cellRenderer) {
		table.getColumnModel().getColumn(columnIdx).setPreferredWidth(columnSize);
		table.getColumnModel().getColumn(columnIdx).setCellRenderer(cellRenderer);
	}

	@Override
	protected void internalRenderTo(Container owner, Object constraints) {
		Preconditions.checkArgument(owner == null || owner instanceof Window,
				"Target must not be specified or be sub-calss of Window");
		Preconditions.checkState(pm != null, "PM is required for this view");

		if (frame != null && frame.getOwner() != owner) {
			frame.remove(panelRoot);
			frame.dispose();
			frame = null;
		}

		if (frame == null) {
			frame = new JFrame();
			frame.setSize(new Dimension(UiUtils.getFontRelativeSize(90), UiUtils.getFontRelativeSize(50)));
			frame.setLayout(new BorderLayout());
			frame.setResizable(true);
			frame.setMinimumSize(new Dimension(UiUtils.getFontRelativeSize(60), UiUtils.getFontRelativeSize(35)));
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			updateWindowTitle();
			frame.add(panelRoot, BorderLayout.CENTER);
			frame.addWindowListener(windowAdapter);
			frame.setJMenuBar(menuBar);

			UiUtils.centerWindow(frame);
			WindowIcon.setWindowIcon(frame);
		}

		frame.setVisible(true);
	}

	protected WindowAdapter windowAdapter = new WindowAdapter() {
		@Override
		public void windowClosing(WindowEvent e) {
			if (isAttached()) {
				pm.getActionConfigExit().actionPerformed(null);
			}
			super.windowClosing(e);
		};
	};

	@Override
	protected void internalUnrender() {
		frame.setVisible(false);
	}

	@Override
	public Window getWindow() {
		return frame;
	}

	public void bringToFront() {
		frame.setVisible(true);
		frame.setState(JFrame.NORMAL);
		UiUtils.makeSureWindowBroughtToFront(frame);
	}

	private abstract class ToolbarAction extends AbstractAction {
		private static final long serialVersionUID = 5177364704498790332L;
		private final String actionNameMessageCode;
		private final String iconFilepathname;
		private Icon icon;

		public ToolbarAction(String actionNameMessageCode, String iconFilepathname) {
			this.actionNameMessageCode = actionNameMessageCode;
			this.iconFilepathname = iconFilepathname;
		}

		@Override
		public Object getValue(String key) {
			if (Action.NAME.equals(key)) {
				return SwingPmSettings.getMessages().get(actionNameMessageCode) + "   ";
			}
			if (Action.SMALL_ICON.equals(key)) {
				return getIcon();
			}
			return super.getValue(key);
		};

		public Icon getIcon() {
			if (icon == null) {
				icon = EntryPoint.loadImage(iconFilepathname);
			}
			return icon;
		}
	}

}
