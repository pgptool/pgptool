package org.pgptool.gui.usage.api;

import java.awt.event.ActionEvent;
import java.io.Serializable;
import javax.swing.Action;
import org.apache.log4j.Logger;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ValueAdapterPersistentPropertyImpl;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;

/**
 * This impl will delegate to right impl based on user choice.
 *
 * @author sergeyk
 */
public class UsageLoggerDelegator implements UsageLogger, HasUsageRecordingSetting {
  public static Logger log = Logger.getLogger(UsageLoggerDelegator.class);

  public static final String IS_USAGE_RECORDING_ENABLED = "IS_USAGE_RECORDING_ENABLED";

  private final UsageLogger noOpImpl;
  private final UsageLogger actualImpl;

  private final ModelProperty<Boolean> isUsageRecordingEnabled;

  public UsageLoggerDelegator(ConfigPairs appProps, UsageLogger noOpImpl, UsageLogger actualImpl) {
    super();
    this.noOpImpl = noOpImpl;
    this.actualImpl = actualImpl;

    isUsageRecordingEnabled =
        new ModelProperty<>(
            this,
            new ValueAdapterPersistentPropertyImpl<>(appProps, IS_USAGE_RECORDING_ENABLED, null),
            IS_USAGE_RECORDING_ENABLED);
    if (isUsageRecordingEnabled.getValue() == null) {
      isUsageRecordingEnabled.setValueByOwner(
          UiUtils.confirmRegular(null, "prompt.doUsageRecording", null));
    }
    log.info("Current value of isUsageRecordingEnabled = " + isUsageRecordingEnabled.getValue());
  }

  @Override
  public <T extends Serializable> void write(T usageEvent) {
    if (Boolean.TRUE.equals(isUsageRecordingEnabled.getValue())) {
      actualImpl.write(usageEvent);
    } else {
      noOpImpl.write(usageEvent);
    }
  }

  private final Action actionToggleUsageRecording =
      new LocalizedActionEx("action.recordUsageInfo", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          isUsageRecordingEnabled.setValueByOwner(!isUsageRecordingEnabled.getValue());
        }
      };

  @Override
  public ModelPropertyAccessor<Boolean> getIsUsageRecordingEnabled() {
    return isUsageRecordingEnabled.getModelPropertyAccessor();
  }

  @Override
  public Action getActionToggleUsageRecording() {
    return actionToggleUsageRecording;
  }
}
