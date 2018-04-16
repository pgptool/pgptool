package org.pgptool.gui.hintsforusage.api;

import org.pgptool.gui.hintsforusage.ui.HintPm;

public interface HintsCoordinator {
	void scheduleHint(HintPm hintPm);

	void cancelHint(HintPm hintPm);

	boolean isHintScheduled(Class<? extends HintPm> hintClazz);

	void setHintsHolder(HintsHolder hintsHolder);

}
