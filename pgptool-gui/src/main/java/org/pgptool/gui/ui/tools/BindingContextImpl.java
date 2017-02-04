package org.pgptool.gui.ui.tools;

import javax.swing.JComponent;

import org.pgptool.gui.ui.validationerrorsballoon.ValidationErrorsBalloonView;

import ru.skarpushin.swingpm.base.HasValidationErrorsListEx;
import ru.skarpushin.swingpm.bindings.BindingContext;

public class BindingContextImpl extends BindingContext {
	@Override
	protected void constructValidationErrorsBinding(HasValidationErrorsListEx validationErrorsSource,
			JComponent component) {
		ValidationErrorsBalloonView veview = new ValidationErrorsBalloonView();
		veview.renderTo(null, component);
		veview.setPm(validationErrorsSource.getValidationErrors());
		add(veview);
	}
}
