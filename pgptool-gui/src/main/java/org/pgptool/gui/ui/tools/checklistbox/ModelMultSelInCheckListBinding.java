package org.pgptool.gui.ui.tools.checklistbox;

import javax.swing.DefaultListModel;

import com.google.common.base.Preconditions;

import ru.skarpushin.swingpm.bindings.Binding;
import ru.skarpushin.swingpm.bindings.BindingContext;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultSelInListProperty;
import ru.skarpushin.swingpm.modelprops.lists.ModelMultSelInListPropertyAccessor;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ModelMultSelInCheckListBinding<E> implements Binding {
	private ModelMultSelInListPropertyAccessor<E> modelProperty;
	private JCheckList list;

	public ModelMultSelInCheckListBinding(BindingContext bindingContext, ModelMultSelInListProperty<E> modelPropertyA,
			JCheckList list) {
		modelProperty = modelPropertyA.getModelMultSelInListPropertyAccessor();
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