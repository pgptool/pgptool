package ru.skarpushin.swingpm.modelprops.EXPORT;

import javax.swing.Action;

import org.pgptool.gui.ui.tools.linkbutton.HasAction;

import ru.skarpushin.swingpm.bindings.Binding;

/**
 * Class for convenience binding action trigger to action
 */
public class HasActionBinding implements Binding {
	private Action action;
	private final HasAction actionTrigger;

	public HasActionBinding(Action action, HasAction actionTrigger) {
		this.action = action;
		this.actionTrigger = actionTrigger;
		actionTrigger.setAction(action);
	}

	@Override
	public boolean isBound() {
		return action != null;
	}

	@Override
	public void unbind() {
		action = null;
		actionTrigger.setAction(action);
	}
}
