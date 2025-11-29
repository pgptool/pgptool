package ru.skarpushin.swingpm.bindings;

import javax.swing.AbstractButton;
import javax.swing.Action;

/** Class for convenience binding action trigger to action */
public class ActionBinding implements Binding {
  private final AbstractButton actionTrigger;
  private Action action;

  public ActionBinding(Action action, AbstractButton actionTrigger) {
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
