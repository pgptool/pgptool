package org.pgptool.gui.usage.api;

import javax.swing.Action;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;

public interface HasUsageRecordingSetting {

  ModelPropertyAccessor<Boolean> getIsUsageRecordingEnabled();

  Action getActionToggleUsageRecording();
}
