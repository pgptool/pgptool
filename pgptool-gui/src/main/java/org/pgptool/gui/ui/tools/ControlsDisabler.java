/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
package org.pgptool.gui.ui.tools;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import ru.skarpushin.swingpm.bindings.TypedPropertyChangeListener;

/**
 * Helper class that will disable all controls on a panel when property value will change to true
 */
public class ControlsDisabler implements TypedPropertyChangeListener<Boolean> {
  private List<Component> disabledComponents = new ArrayList<>();
  private JPanel rootPanelToDisable;

  public ControlsDisabler(JPanel rootPanelToDisable) {
    this.rootPanelToDisable = rootPanelToDisable;
  }

  /**
   * NOTE: This logic is not perfect! Can be used only if there is no race condition possible (like
   * component was disabled from outside while operation is in progress. In that case this component
   * must not be enabled. But this case is not handled in current impl. COPY-PASTE CAREFULLY
   */
  @Override
  public void handlePropertyChanged(
      Object source, String propertyName, Boolean oldValue, Boolean isDisableControls) {
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
