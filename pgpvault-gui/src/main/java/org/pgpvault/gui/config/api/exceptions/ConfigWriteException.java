package org.pgpvault.gui.config.api.exceptions;

import org.summerb.approaches.i18n.HasMessageArgs;
import org.summerb.approaches.i18n.HasMessageCode;

public class ConfigWriteException extends Exception implements HasMessageCode, HasMessageArgs {
	private static final long serialVersionUID = 1253040402059386002L;

	private final String propertyFileLocation;

	public ConfigWriteException(String propertyFileLocation, String technicalMessage, Throwable cause) {
		super(technicalMessage, cause);
		this.propertyFileLocation = propertyFileLocation;
	}

	@Override
	public String getMessageCode() {
		return "exceptions.unexpected.failedToSaveConfiguration";
	}

	@Override
	public Object[] getMessageArgs() {
		return new Object[] { propertyFileLocation };
	}
}
