package org.pgptool.gui.ui.tools.geometrymemory;

import ru.skarpushin.swingpm.base.Detachable;

public interface WindowGeometryPersister extends Detachable {

	/**
	 * @return true if config values found and applied
	 */
	boolean restoreSize();

	// /**
	// * TODO: I think settings location forcibly could be dangerous, it might not
	// be
	// * what user expects. Let's closely look at it and I might revert this feature
	// *
	// * @return true if config values found and applied
	// */
	// boolean restoreLocation();
}