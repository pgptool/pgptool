package org.pgptool.gui.ui.tools;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.apache.log4j.Logger;

import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;

/**
 * Helper class that will disable all controls on a panel when property value
 * will change to true
 */
public class ControlsDisabler implements TypedPropertyChangeListener<Boolean> {
	private static Logger log = Logger.getLogger(ControlsDisabler.class);
	
	private List<Component> disabledComponents = new ArrayList<>();
	private JPanel rootPanelToDisable;

	public ControlsDisabler(JPanel rootPanelToDisable) {
		this.rootPanelToDisable = rootPanelToDisable;
	}

	/**
	 * NOTE: This logic is not perfect! Can be used only if there is no race
	 * condition possible (like component was disabled from outside while operation
	 * is in progress. In that case this component must not be enabled. But this
	 * case is not handled in current impl. COPY-PASTE CAREFULLY
	 */
	@Override
	public void handlePropertyChanged(Object source, String propertyName, Boolean oldValue, Boolean isDisableControls) {
		if (isDisableControls) {
			disableForm(rootPanelToDisable);
		} else {
			while (!disabledComponents.isEmpty()) {
				disabledComponents.remove(0).setEnabled(true);
			}
		}
		rootPanelToDisable.revalidate();
	}

	public void disableForm(Container container) {
		Component[] components = container.getComponents();
		for (Component component : components) {
			if (component.isEnabled()) {
				component.setEnabled(false);
				disabledComponents.add(component);
			}

			if (component instanceof Container) {
				disableForm((Container) component);
			}
		}
	}
}