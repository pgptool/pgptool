package ru.skarpushin.swingpm.tools.i18n;

public class MessagesProviderNoOpImpl implements MessagesProvider {
  @Override
  public String get(String messageCode) {
    return messageCode;
  }

  @Override
  public String get(String messageCode, Object... args) {
    return messageCode;
  }
}
