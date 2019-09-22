package org.pgptool.gui.usage.api;

import java.io.Serializable;

/**
 * This impl of {@link UsageLogger} will be used if user decided not to collect
 * usage data.
 * 
 * @author sergeyk
 *
 */
public class UsageLoggerNoOpImpl implements UsageLogger {
	@Override
	public <T extends Serializable> void write(T usageEvent) {
		// no operation -- empty method
	}

}
