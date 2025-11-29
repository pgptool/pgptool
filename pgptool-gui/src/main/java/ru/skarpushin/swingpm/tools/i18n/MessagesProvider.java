package ru.skarpushin.swingpm.tools.i18n;

/**
 * Interface to convert message codes into user-readable text
 *
 * @author sergeyk
 */
public interface MessagesProvider {
  String get(String messageCode);

  String get(String messageCode, Object... args);
}
