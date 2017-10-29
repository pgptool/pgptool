package ru.skarpushin.swingpm.modelprops.slider;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.summerb.approaches.validation.ValidationError;

import ru.skarpushin.swingpm.collections.ListEx;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

/**
 * Model property for Slider
 * 
 * NOTE: This impl is very simple and limited. I.e. it doens't support extent
 * and doesn't perform all those checks performed by
 * {@link DefaultBoundedRangeModel}
 * 
 * @author sergeyk
 *
 */
public class ModelSliderProperty extends ModelProperty<Integer> {
	private int minimum;
	private int maximum;

	protected List<ChangeListener> changeEventListeners = new ArrayList<>();

	public ModelSliderProperty(Object source, ValueAdapter<Integer> valueAdapter, int minimum, int maximum,
			String propertyName) {
		this(source, valueAdapter, minimum, maximum, propertyName, null);
	}

	public ModelSliderProperty(Object source, ValueAdapter<Integer> valueAdapter, int minimum, int maximum,
			String propertyName, ListEx<ValidationError> veSource) {
		super(source, valueAdapter, propertyName, veSource);
		this.minimum = minimum;
		this.maximum = maximum;

		getModelPropertyAccessor().addPropertyChangeListener(wrapperForChangeListener);
	}

	private PropertyChangeListener wrapperForChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			fireStateChanged();
		}
	};

	/**
	 * NOTE: This is another notification "road". We have to implement it because
	 * it's part of BoundedRangeModel contract
	 */
	protected void fireStateChanged() {
		ChangeEvent changeEvent = null;
		for (ChangeListener listener : changeEventListeners) {
			if (changeEvent == null) {
				changeEvent = new ChangeEvent(source);
			}
			listener.stateChanged(changeEvent);
		}
	}

	public ModelSliderPropertyAccessor getModelSliderPropertyAccessor() {
		return modelSliderPropertyAccessor;
	}

	private final ModelSliderPropertyAccessor modelSliderPropertyAccessor = new ModelSliderPropertyAccessor() {
		@Override
		public ListEx<ValidationError> getValidationErrors() {
			return getModelPropertyAccessor().getValidationErrors();
		}

		@Override
		public String getPropertyName() {
			return getModelPropertyAccessor().getPropertyName();
		}

		@Override
		public void setValue(Integer value) {
			getModelPropertyAccessor().setValue(value);
		}

		@Override
		public Integer getValue() {
			return getModelPropertyAccessor().getValue();
		}

		@Override
		public void removePropertyChangeListener(PropertyChangeListener propertyChangeBoundHandler) {
			getModelPropertyAccessor().removePropertyChangeListener(propertyChangeBoundHandler);
		}

		@Override
		public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener) {
			getModelPropertyAccessor().addPropertyChangeListener(propertyChangeListener);
		}

		@Override
		public BoundedRangeModel getBoundedRangeModel() {
			return boundedRangeModel;
		}
	};

	private final BoundedRangeModel boundedRangeModel = new BoundedRangeModel() {
		private boolean valueIsAdjusting;

		@Override
		public int getMinimum() {
			return minimum;
		}

		@Override
		public void setMinimum(int minimum) {
			throw new IllegalStateException("Operation is not supported by this impl");
		}

		@Override
		public int getMaximum() {
			return maximum;
		}

		@Override
		public void setMaximum(int maximum) {
			throw new IllegalStateException("Operation is not supported by this impl");
		}

		@Override
		public boolean getValueIsAdjusting() {
			return valueIsAdjusting;
		}

		@Override
		public void setValueIsAdjusting(boolean valueIsAdjusting) {
			this.valueIsAdjusting = valueIsAdjusting;
			fireStateChanged();
		}

		@Override
		public int getValue() {
			return ModelSliderProperty.this.getValue() == null ? 0 : ModelSliderProperty.this.getValue();
		}

		@SuppressWarnings("deprecation")
		@Override
		public void setValue(int newValue) {
			ModelSliderProperty.this.setValueByConsumer(newValue);
		}

		@Override
		public int getExtent() {
			return 0;
		}

		@Override
		public void setExtent(int newExtent) {
			throw new IllegalStateException("Operation is not supported by this impl");
		}

		@Override
		public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
			setValueIsAdjusting(adjusting);
			setMinimum(min);
			setMaximum(max);
			setValue(value);
			setExtent(extent);
		}

		@Override
		public void addChangeListener(ChangeListener x) {
			changeEventListeners.add(x);
		}

		@Override
		public void removeChangeListener(ChangeListener x) {
			changeEventListeners.remove(x);
		}
	};

}
