package org.pgpvault.gui.ui.mainframe;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.ui.tools.UiUtils;
import org.pgpvault.gui.ui.tools.WindowIcon;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.base.HasWindow;
import ru.skarpushin.swingpm.base.ViewBase;

public class MainFrameView extends ViewBase<MainFramePm> implements HasWindow {
	private JFrame frame;

	private JPanel panelRoot;

	private JMenuBar menuBar;
	private JMenuItem miPgpImportKey;
	private JMenuItem miShowKeyList;
	private JMenuItem miChangeTempFolderForDecrypted;
	private JMenuItem miAbout;
	private JMenuItem miConfigExit;
	private JMenuItem miEncrypt;
	private JMenuItem miDecrypt;

	@Override
	protected void internalInitComponents() {
		panelRoot = new JPanel(new BorderLayout());

		initMenuBar();
		initFormComponents();
	}

	private void initFormComponents() {
		JLabel tbd = new JLabel("!             TBD             !", SwingConstants.CENTER);
		panelRoot.add(tbd, BorderLayout.CENTER);
	}

	private void initMenuBar() {
		menuBar = new JMenuBar();

		JMenu menuFile = new JMenu(Messages.get("term.appTitle"));
		menuFile.add(miPgpImportKey = new JMenuItem());
		menuFile.add(miShowKeyList = new JMenuItem());
		menuFile.addSeparator();
		menuFile.add(miChangeTempFolderForDecrypted = new JMenuItem());
		menuFile.addSeparator();
		menuFile.add(miAbout = new JMenuItem());
		menuFile.add(miConfigExit = new JMenuItem());

		JMenu menuActions = new JMenu(Messages.get("term.actions"));
		menuActions.add(miEncrypt = new JMenuItem());
		menuActions.add(miDecrypt = new JMenuItem());

		menuBar.add(menuFile);
		menuBar.add(menuActions);
	}

	@Override
	protected void internalBindToPm() {
		super.internalBindToPm();

		updateWindowTitle();
		bindToActions();
	}

	private void bindToActions() {
		bindingContext.setupBinding(pm.getActionConfigExit(), miConfigExit);
		bindingContext.setupBinding(pm.getActionAbout(), miAbout);
		bindingContext.setupBinding(pm.getActionImportKey(), miPgpImportKey);
		bindingContext.setupBinding(pm.getActionShowKeysList(), miShowKeyList);
		bindingContext.setupBinding(pm.getActionChangeFolderForDecrypted(), miChangeTempFolderForDecrypted);
		bindingContext.setupBinding(pm.getActionEncrypt(), miEncrypt);
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
			frame.setSize(new Dimension(UiUtils.getFontRelativeSize(70), UiUtils.getFontRelativeSize(30)));
			frame.setLayout(new BorderLayout());
			frame.setResizable(true);
			frame.setMinimumSize(new Dimension(UiUtils.getFontRelativeSize(60), UiUtils.getFontRelativeSize(25)));
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
		frame.toFront();
	}
}
