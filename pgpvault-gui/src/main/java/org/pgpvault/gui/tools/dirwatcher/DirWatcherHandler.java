package org.pgpvault.gui.tools.dirwatcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

public interface DirWatcherHandler {
	/**
	 * This method is invoked when watcher can no longer continue operation
	 * mostly this happens if watched dir was removed
	 */
	void watcherHasToStop();

	void handleEvent(WatchEvent<?> event, Path node);
}
