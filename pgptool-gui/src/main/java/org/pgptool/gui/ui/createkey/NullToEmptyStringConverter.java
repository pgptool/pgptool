package org.pgptool.gui.ui.createkey;

import ru.skarpushin.swingpm.valueadapters.ConversionValueAdapter;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

public class NullToEmptyStringConverter extends ConversionValueAdapter<String, String> {
  public NullToEmptyStringConverter(ValueAdapter<String> innerValueAdapter) {
    super(innerValueAdapter);
  }

  @Override
  protected String convertInnerToOuter(String value) {
    return value != null ? value : "";
  }

  @Override
  protected String convertOuterToInner(String value) {
    return value != null ? value : "";
  }
}
