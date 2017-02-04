package org.pgptool.gui.app;

import org.summerb.approaches.i18n.HasMessageArgs;
import org.summerb.approaches.i18n.HasMessageCode;

public class GenericException extends Exception implements HasMessageCode, HasMessageArgs {
	private static final long serialVersionUID = 5911368838530147923L;
	private Object[] messageArgs;

	public GenericException(String messageCode) {
		super(messageCode);
	}

	public GenericException(String messageCode, Throwable cause, Object... messageArgs) {
		super(messageCode, cause);
		this.messageArgs = messageArgs;
	}

	@Override
	public String getMessageCode() {
		return getMessage();
	}

	@Override
	public Object[] getMessageArgs() {
		return messageArgs;
	}

}
