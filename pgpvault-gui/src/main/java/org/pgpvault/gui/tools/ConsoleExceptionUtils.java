package org.pgpvault.gui.tools;

import java.util.Locale;

import org.pgpvault.gui.app.EntryPoint;
import org.pgpvault.gui.app.Messages;
import org.springframework.context.ApplicationContext;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.summerb.approaches.i18n.HasMessageCode;
import org.summerb.approaches.i18n.I18nUtils;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationError;

public class ConsoleExceptionUtils {
	public static String getAllMessages(Throwable t) {
		if (t == null)
			return "";

		StringBuffer ret = new StringBuffer();

		Throwable cur = t;
		while (cur != null) {
			if (cur == cur.getCause())
				break;

			if (ret.length() > 0) {
				ret.append(" -> ");
			}

			if (cur instanceof FieldValidationException) {
				FieldValidationException fve = (FieldValidationException) cur;
				ret.append(buildMessageForFve(fve, LocaleContextHolder.getLocale()));
			} else if (cur instanceof HasMessageCode) {
				ret.append(I18nUtils.buildMessage((HasMessageCode) cur, ac()));
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

	protected static StringBuilder buildMessageForFve(FieldValidationException fve, Locale locale) {
		StringBuilder ret = new StringBuilder();
		ret.append(I18nUtils.buildMessage(fve, ac()));
		ret.append(": ");
		boolean first = true;
		for (ValidationError ve : fve.getErrors()) {
			if (!first) {
				ret.append(", ");
			}
			ret.append("\"" + tryFindTranslation(ve.getFieldToken(), locale) + "\"");
			ret.append(" - ");
			ret.append(I18nUtils.buildMessage(ve, ac()));
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
