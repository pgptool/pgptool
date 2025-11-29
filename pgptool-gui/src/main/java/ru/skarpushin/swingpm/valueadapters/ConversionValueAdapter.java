package ru.skarpushin.swingpm.valueadapters;

/**
 * @author sergeyk
 * @param <TInner> this most likely closer to POJO (DTO)
 * @param <TOuter> this most likely closer to PM/View
 */
public abstract class ConversionValueAdapter<TInner, TOuter> implements ValueAdapter<TOuter> {
  protected final ValueAdapter<TInner> innerValueAdapter;

  public ConversionValueAdapter(ValueAdapter<TInner> innerValueAdapter) {
    this.innerValueAdapter = innerValueAdapter;
  }

  @Override
  public TOuter getValue() {
    return convertInnerToOuter(innerValueAdapter.getValue());
  }

  @Override
  public void setValue(TOuter value) {
    innerValueAdapter.setValue(convertOuterToInner(value));
  }

  protected abstract TOuter convertInnerToOuter(TInner value);

  protected abstract TInner convertOuterToInner(TOuter value);
}
