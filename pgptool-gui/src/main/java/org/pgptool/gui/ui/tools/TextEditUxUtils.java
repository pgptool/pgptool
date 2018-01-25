package org.pgptool.gui.ui.tools;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.pgptool.gui.app.Messages;

public class TextEditUxUtils {

	public static class Cut extends TextAction {
		private static final long serialVersionUID = 1L;
		private JTextComponent component;

		public Cut(JTextComponent component) {
			super(Messages.get("text.edit.cut"));
			this.component = component;
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			component.cut();
		}
	}

	public static class Copy extends TextAction {
		private static final long serialVersionUID = 1L;
		private JTextComponent component;

		public Copy(JTextComponent component) {
			super(Messages.get("text.edit.copy"));
			this.component = component;
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			component.copy();
		}
	}

	public static class Paste extends TextAction {
		private static final long serialVersionUID = 1L;
		private JTextComponent component;

		public Paste(JTextComponent component) {
			super(Messages.get("text.edit.paste"));
			this.component = component;
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			component.paste();
		}
	}

	public static class SelectAll extends TextAction {
		private static final long serialVersionUID = 1L;
		private JTextComponent component;

		public SelectAll(JTextComponent component) {
			super(Messages.get("text.edit.selectAll"));
			this.component = component;
			putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control A"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			component.selectAll();
			component.requestFocusInWindow();
		}
	}

	public static void setCommonTextEditorActions(JTextComponent textComponent) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(new Cut(textComponent));
		menu.add(new Copy(textComponent));
		menu.add(new Paste(textComponent));
		menu.addSeparator();
		menu.add(new SelectAll(textComponent));
		textComponent.setComponentPopupMenu(menu);

		setHistoryActions(textComponent);
	}

	private static void setHistoryActions(JTextComponent textComponent) {
		Document doc = textComponent.getDocument();
		UndoManager undo = new UndoManager();
		doc.addUndoableEditListener(new UndoableEditListener() {
			@Override
			public void undoableEditHappened(UndoableEditEvent evt) {
				undo.addEdit(evt.getEdit());
			}
		});

		textComponent.getActionMap().put("Undo", new AbstractAction("Undo") {
			private static final long serialVersionUID = 2048749213313143683L;

			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canUndo()) {
						undo.undo();
					}
				} catch (CannotUndoException e) {
				}
			}
		});
		textComponent.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");

		textComponent.getActionMap().put("Redo", new AbstractAction("Redo") {
			private static final long serialVersionUID = 2048749213313143683L;

			@Override
			public void actionPerformed(ActionEvent evt) {
				try {
					if (undo.canRedo()) {
						undo.redo();
					}
				} catch (CannotRedoException e) {
				}
			}
		});
		textComponent.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
	}
}
