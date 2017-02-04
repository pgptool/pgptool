package org.pgptool.gui.ui.tools;

import ru.skarpushin.swingpm.bindings.BindingContext;
import ru.skarpushin.swingpm.bindings.BindingContextFactory;

public class BindingContextFactoryImpl implements BindingContextFactory {
	@Override
	public BindingContext buildContext() {
		return new BindingContextImpl();
	}
}
