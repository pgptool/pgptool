package org.pgpvault.gui.app;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.i18n.LocaleContextHolder;

import ru.skarpushin.swingpm.tools.SwingPmSettings;
import ru.skarpushin.swingpm.tools.i18n.MessagesProvider;

public class Messages implements ApplicationContextAware {
	private static Logger log = Logger.getLogger(Messages.class);

	private ApplicationContext applicationContext;
	private static Messages INSTANCE;

	public Messages() {
		// NOTE: It's ok, because we have only one singleton this bean instance
		INSTANCE = this;
		SwingPmSettings.setMessages(messagesProvider);
	}

	public static String get(String messageCode) {
		return get(messageCode, (Object[]) null);
	}

	public static String get(String messageCode, Object... args) {
		if (INSTANCE == null || INSTANCE.applicationContext == null) {
			return messageCode;
		}

		try {
			return INSTANCE.applicationContext.getMessage(messageCode, args, LocaleContextHolder.getLocale());
		} catch (Throwable t) {
			log.warn("Failed to get message: " + messageCode);
			return messageCode;
		}
	}

	private static MessagesProvider messagesProvider = new MessagesProvider() {
		@Override
		public String get(String messageCode, Object... args) {
			return Messages.get(messageCode, args);
		}

		@Override
		public String get(String messageCode) {
			return Messages.get(messageCode);
		}
	};

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}
