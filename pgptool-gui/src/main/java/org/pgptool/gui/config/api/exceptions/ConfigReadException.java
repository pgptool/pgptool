package org.pgptool.gui.config.api.exceptions;

import org.summerb.approaches.i18n.HasMessageArgs;
import org.summerb.approaches.i18n.HasMessageCode;

public class ConfigReadException extends Exception implements HasMessageCode, HasMessageArgs {
	private static final long serialVersionUID = 4233271701232842609L;
	private final String propertyFileLocation;

	public ConfigReadException(String propertyFileLocation, String technicalMessage, Throwable cause) {
		super(technicalMessage, cause);
		this.propertyFileLocation = propertyFileLocation;
	}

	@Override
	public String getMessageCode() {
		return "exceptions.unexpected.failedToLoadConfiguration";
	}

	@Override
	public Object[] getMessageArgs() {
		return new Object[] { propertyFileLocation };
	}
}
