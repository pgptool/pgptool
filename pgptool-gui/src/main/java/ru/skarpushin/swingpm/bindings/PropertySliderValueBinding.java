package ru.skarpushin.swingpm.bindings;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JSlider;

import ru.skarpushin.swingpm.modelprops.slider.ModelSliderPropertyAccessor;

public class PropertySliderValueBinding implements Binding {
	private final ModelSliderPropertyAccessor property;
	private JSlider slider;

	public PropertySliderValueBinding(ModelSliderPropertyAccessor property, JSlider slider) {
		this.property = property;
		this.slider = slider;

		slider.setModel(property.getBoundedRangeModel());
	}

	@Override
	public boolean isBound() {
		return slider.getModel() == property.getBoundedRangeModel();
	}

	@Override
	public void unbind() {
		slider.setModel(new DefaultBoundedRangeModel());
	}
}
