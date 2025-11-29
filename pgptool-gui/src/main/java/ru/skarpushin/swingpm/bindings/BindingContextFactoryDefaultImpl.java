package ru.skarpushin.swingpm.bindings;

public class BindingContextFactoryDefaultImpl implements BindingContextFactory {
  @Override
  public BindingContext buildContext() {
    return new BindingContext();
  }
}
