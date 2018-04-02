package org.pgptool.gui.ui.historyquicksearch;

import org.pgptool.gui.ui.decryptone.DecryptionDialogParameters;

public interface HistoryQuickSearchHost {
	void handleChosen(DecryptionDialogParameters optionalTsRecordSubject);

	void handleCancel();
}
