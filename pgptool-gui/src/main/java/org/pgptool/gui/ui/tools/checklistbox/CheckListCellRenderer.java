package org.pgptool.gui.ui.tools.checklistbox;

import java.awt.Component;
import java.awt.Dimension;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.border.Border;

/**
 * TBD: This impl is not even close to the capabilities of
 * {@link DefaultListCellRenderer}, but it's ok for now
 * 
 * @author sergeyk
 */
public class CheckListCellRenderer<E> extends JPanel implements ListCellRenderer<E> {
	private static final long serialVersionUID = 280251519708691650L;

	protected JCheckBox check;
	protected JLabel label;

	protected Border emptyBorder;
	protected Border checkedBorder;
	protected Border focusedBorder;

	private Function<E, Boolean> checkStateSupplier;

	public CheckListCellRenderer(Function<E, Boolean> checkStateSupplier) {
		this.checkStateSupplier = checkStateSupplier;
		setLayout(null);

		add(check = new JCheckBox());
		add(label = new JLabel());

		check.setBackground(UIManager.getColor("List.textBackground"));
		label.setOpaque(false);
		check.setOpaque(false);

		emptyBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1);
		focusedBorder = BorderFactory.createLineBorder(UIManager.getColor("List.focusSelectedCellHighlightBorder"), 1);

		setBorder(emptyBorder);
	}

	@Override
	public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected,
			boolean cellHasFocus) {
		if (checkedBorder == null) {
			checkedBorder = BorderFactory.createLineBorder(list.getSelectionBackground(), 1);
		}

		setEnabled(list.isEnabled());

		check.setSelected(checkStateSupplier.apply(value));
		check.setEnabled(list.isEnabled());

		label.setFont(list.getFont());
		label.setText(value.toString());
		label.setEnabled(list.isEnabled());

		boolean isChecked = check.isSelected();
		if (isChecked) {
			if (cellHasFocus) {
				setBorder(focusedBorder);
			} else {
				setBorder(checkedBorder);
			}

			setBackground(list.getSelectionBackground());
			label.setForeground(list.getSelectionForeground());
		} else {
			if (cellHasFocus) {
				setBorder(focusedBorder);
			} else {
				setBorder(emptyBorder);
			}

			setBackground(list.getBackground());
			label.setForeground(list.getForeground());
		}

		return this;
	}

	@Override
	public Dimension getPreferredSize() {
		Dimension dCheck = check.getPreferredSize();
		Dimension dLabel = label.getPreferredSize();
		return new Dimension(dCheck.width + dLabel.width,
				(dCheck.height < dLabel.height ? dLabel.height : dCheck.height));
	}

	@Override
	public void doLayout() {
		Dimension dCheck = check.getPreferredSize();
		Dimension dLabel = label.getPreferredSize();
		int yCheck = 0;
		int yLabel = 0;
		if (dCheck.height < dLabel.height) {
			yCheck = (dLabel.height - dCheck.height) / 2;
		} else {
			yLabel = (dCheck.height - dLabel.height) / 2;
		}
		check.setLocation(0, yCheck);
		check.setBounds(0, yCheck, dCheck.width, dCheck.height);
		label.setLocation(dCheck.width, yLabel);
		label.setBounds(dCheck.width, yLabel, dLabel.width, dLabel.height);
	}
}