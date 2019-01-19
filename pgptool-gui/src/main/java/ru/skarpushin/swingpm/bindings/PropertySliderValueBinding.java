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
