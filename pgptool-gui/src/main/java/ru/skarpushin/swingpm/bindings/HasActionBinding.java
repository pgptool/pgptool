package ru.skarpushin.swingpm.bindings;

import javax.swing.Action;

/** Class for convenience binding action trigger to action */
public class HasActionBinding implements Binding {
  private final HasAction actionTrigger;
  private Action action;

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
