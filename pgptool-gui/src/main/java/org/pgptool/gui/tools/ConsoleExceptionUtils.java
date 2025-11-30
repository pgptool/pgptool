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
package org.pgptool.gui.tools;

import java.util.Locale;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.Messages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.summerb.i18n.HasMessageCode;
import org.summerb.i18n.I18nUtils;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationException;

public class ConsoleExceptionUtils {
  public static String getAllMessages(Throwable t) {
    if (t == null) {
      return "";
    }

    StringBuilder ret = new StringBuilder();
    Locale locale = LocaleContextHolder.getLocale();

    Throwable cur = t;
    while (cur != null) {
      if (cur == cur.getCause()) break;

      if (!ret.isEmpty()) {
        ret.append(" -> ");
      }

      if (cur instanceof ValidationException fve) {
        ret.append(buildMessageForFve(fve, locale));
      } else if (cur instanceof HasMessageCode) {
        ret.append(I18nUtils.buildMessage((HasMessageCode) cur, ac(), locale));
      } else {
        try {
          String className = cur.getClass().getName();
          String messageMappingForClassName = Messages.get(className, cur.getMessage());
          if (className.equals(messageMappingForClassName)) {
            throw new NoSuchMessageException(className);
          }
          ret.append(messageMappingForClassName);
        } catch (NoSuchMessageException nfe) {
          ret.append(cur.getLocalizedMessage());
        }
      }

      cur = cur.getCause();
    }

    return ret.toString();
  }

  private static ApplicationContext ac() {
    return EntryPoint.INSTANCE.getApplicationContext();
  }

  protected static StringBuilder buildMessageForFve(ValidationException fve, Locale locale) {
    StringBuilder ret = new StringBuilder();
    ret.append(I18nUtils.buildMessage(fve, ac(), locale));
    ret.append(": ");
    boolean first = true;
    for (ValidationError ve : fve.getErrors()) {
      if (!first) {
        ret.append(", ");
      }
      ret.append("\"").append(tryFindTranslation(ve.getPropertyName(), locale)).append("\"");
      ret.append(" - ");
      ret.append(I18nUtils.buildMessage(ve, ac(), locale));
      first = false;
    }
    return ret;
  }

  private static Object tryFindTranslation(String fieldToken, Locale locale) {
    try {
      return ac().getMessage(fieldToken, null, locale);
    } catch (NoSuchMessageException nfe) {
      try {
        return ac().getMessage("term." + fieldToken, null, locale);
      } catch (NoSuchMessageException nfe2) {
        return fieldToken;
      }
    }
  }
}
