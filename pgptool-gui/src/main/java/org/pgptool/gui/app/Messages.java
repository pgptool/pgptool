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
package org.pgptool.gui.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import ru.skarpushin.swingpm.tools.SwingPmSettings;
import ru.skarpushin.swingpm.tools.i18n.MessagesProvider;

public class Messages {
  private static final Logger log = LoggerFactory.getLogger(Messages.class);

  private final ApplicationContext applicationContext;

  private static Messages INSTANCE;

  public Messages(ApplicationContext applicationContext) {
    this.applicationContext = applicationContext;
    // NOTE: It's ok, because we have only one singleton this bean instance
    INSTANCE = this;
    SwingPmSettings.setMessages(messagesProvider);
  }

  public static String get(String messageCode) {
    return get(messageCode, (Object[]) null);
  }

  /**
   * Convert message code to display text. Same as {@link #get(String)}, but more self-descriptive
   * in case static import is used
   */
  public static String text(String messageCode) {
    return get(messageCode, (Object[]) null);
  }

  /**
   * Convert message code to display text. Same as {@link #get(String, Object...)}, but more
   * self-descriptive in case static import is used
   */
  public static String text(String messageCode, Object... args) {
    return get(messageCode, args);
  }

  public static String text(Message msg) {
    return get(msg.getMessageCode(), msg.getMessageArgs());
  }

  public static String get(String messageCode, Object... args) {
    if (INSTANCE == null || INSTANCE.applicationContext == null) {
      return messageCode;
    }

    try {
      return INSTANCE.applicationContext.getMessage(
          messageCode, args, LocaleContextHolder.getLocale());
    } catch (Throwable t) {
      log.warn("Failed to get message: {}", messageCode);
      return messageCode;
    }
  }

  private static final MessagesProvider messagesProvider =
      new MessagesProvider() {
        @Override
        public String get(String messageCode, Object... args) {
          return Messages.get(messageCode, args);
        }

        @Override
        public String get(String messageCode) {
          return Messages.get(messageCode);
        }
      };
}
