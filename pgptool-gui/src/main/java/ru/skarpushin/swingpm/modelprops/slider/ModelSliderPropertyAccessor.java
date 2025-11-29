package ru.skarpushin.swingpm.modelprops.slider;

import javax.swing.BoundedRangeModel;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;

public interface ModelSliderPropertyAccessor extends ModelPropertyAccessor<Integer> {
  BoundedRangeModel getBoundedRangeModel();
}
