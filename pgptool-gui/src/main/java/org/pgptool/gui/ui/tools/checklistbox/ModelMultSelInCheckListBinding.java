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
package org.pgptool.gui.ui.tools.checklistbox;

import com.google.common.base.Preconditions;
import javax.swing.DefaultListModel;
import ru.skarpushin.swingpm.bindings.Binding;
import ru.skarpushin.swingpm.bindings.BindingContext;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultiSelInListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultiSelInListPropertyAccessor;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ModelMultSelInCheckListBinding<E> implements Binding {
  private ModelMultiSelInListPropertyAccessor<E> modelProperty;
  private JCheckList list;

  public ModelMultSelInCheckListBinding(
      BindingContext bindingContext,
      ModelMultiSelInListProperty<E> modelPropertyA,
      JCheckList list) {
    modelProperty = modelPropertyA.getModelMultiSelInListPropertyAccessor();
    this.list = list;

    list.setModel(modelProperty.getOptions());
    list.setCheckState(modelPropertyA.getList());

    bindingContext.createValidationErrorsViewIfAny(modelProperty, list);
  }

  @Override
  public boolean isBound() {
    return modelProperty != null;
  }

  @Override
  public void unbind() {
    Preconditions.checkState(modelProperty != null);

    list.setModel(new DefaultListModel());
    list.setCheckState(null);
    list = null;
    modelProperty = null;
  }
}
