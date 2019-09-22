package org.pgptool.gui.usage.api;

import java.io.Serializable;

public interface UsageLogger {
	<T extends Serializable> void write(T usageEvent);
}
