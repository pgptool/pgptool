package org.pgptool.gui.ui.mainframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

public class EncryptBackActionCellRenderer extends JPanel implements TableCellRenderer {
	private static final long serialVersionUID = 1821194884019502554L;

	private static final Border SAFE_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
	private static final Border DEFAULT_NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);
	protected static Border noFocusBorder = DEFAULT_NO_FOCUS_BORDER;

	private JLabel lbl;

	private Color unselectedForeground;
	private Color unselectedBackground;

	public EncryptBackActionCellRenderer() {
		BorderLayout layout = new BorderLayout();
		setLayout(layout);
		lbl = new JLabel("Encrypt back");
		lbl.setForeground(Color.blue);
		lbl.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
		add(lbl, BorderLayout.WEST);
		setBorder(getNoFocusBorder());
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (table == null) {
			return this;
		}

		lbl.setFont(table.getFont().deriveFont(Font.BOLD));

		Color fg = null;
		Color bg = null;

		JTable.DropLocation dropLocation = table.getDropLocation();
		if (dropLocation != null && !dropLocation.isInsertRow() && !dropLocation.isInsertColumn()
				&& dropLocation.getRow() == row && dropLocation.getColumn() == column) {
			fg = UIManager.getColor("Table.dropCellForeground");
			bg = UIManager.getColor("Table.dropCellBackground");
			isSelected = true;
		}

		if (isSelected) {
			super.setForeground(fg == null ? table.getSelectionForeground() : fg);
			super.setBackground(bg == null ? table.getSelectionBackground() : bg);
		} else {
			Color background = unselectedBackground != null ? unselectedBackground : table.getBackground();
			if (background == null || background instanceof javax.swing.plaf.UIResource) {
				Color alternateColor = UIManager.getColor("Table.alternateRowColor");
				if (alternateColor != null && row % 2 != 0) {
					background = alternateColor;
				}
			}
			super.setForeground(unselectedForeground != null ? unselectedForeground : table.getForeground());
			super.setBackground(background);
		}

		setFont(table.getFont());

		if (hasFocus) {
			Border border = null;
			if (isSelected) {
				border = UIManager.getBorder("Table.focusSelectedCellHighlightBorder");
			}
			if (border == null) {
				border = UIManager.getBorder("Table.focusCellHighlightBorder");
			}
			setBorder(border);

			if (!isSelected && table.isCellEditable(row, column)) {
				Color col;
				col = UIManager.getColor("Table.focusCellForeground");
				if (col != null) {
					super.setForeground(col);
				}
				col = UIManager.getColor("Table.focusCellBackground");
				if (col != null) {
					super.setBackground(col);
				}
			}
		} else {
			setBorder(getNoFocusBorder());
		}

		return this;
	}

	private Border getNoFocusBorder() {
		Border border = UIManager.getBorder("Table.cellNoFocusBorder");
		if (System.getSecurityManager() != null) {
			if (border != null)
				return border;
			return SAFE_NO_FOCUS_BORDER;
		} else if (border != null) {
			if (noFocusBorder == null || noFocusBorder == DEFAULT_NO_FOCUS_BORDER) {
				return border;
			}
		}
		return noFocusBorder;
	}

	/**
	 * Overrides <code>JComponent.setForeground</code> to assign the
	 * unselected-foreground color to the specified color.
	 *
	 * @param c
	 *            set the foreground color to this value
	 */
	@Override
	public void setForeground(Color c) {
		super.setForeground(c);
		unselectedForeground = c;
	}

	/**
	 * Overrides <code>JComponent.setBackground</code> to assign the
	 * unselected-background color to the specified color.
	 *
	 * @param c
	 *            set the background color to this value
	 */
	@Override
	public void setBackground(Color c) {
		super.setBackground(c);
		unselectedBackground = c;
	}

	/**
	 * Notification from the <code>UIManager</code> that the look and feel [L&amp;F]
	 * has changed. Replaces the current UI object with the latest version from the
	 * <code>UIManager</code>.
	 *
	 * @see JComponent#updateUI
	 */
	@Override
	public void updateUI() {
		super.updateUI();
		setForeground(null);
		setBackground(null);
	}
}
