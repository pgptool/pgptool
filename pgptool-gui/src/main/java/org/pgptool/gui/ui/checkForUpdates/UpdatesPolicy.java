/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package org.pgptool.gui.ui.checkForUpdates;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.Action;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.root.RootPm.CheckForUpdatesDialog;
import org.pgptool.gui.ui.tools.UiUtils;
import org.pgptool.gui.ui.tools.browsefs.ValueAdapterPersistentPropertyImpl;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapter;

public class UpdatesPolicy {
  public static final String UPDATES_POLICY = "UpdatesPolicy:";
  public static final String PROP_IS_AUTO = UPDATES_POLICY + "IsAuto";
  public static final String PROP_SNOOZED_VERSION = UPDATES_POLICY + "SnoozedVersion";

  private final ConfigPairs appProps;
  private final ApplicationContext applicationContext;
  private CheckForUpdatesPm checkForUpdatesPm;
  private CheckForUpdatesDialog checkForUpdatesDialog;

  private ModelProperty<Boolean> isAutoUpdatesEnabled;
  private ValueAdapter<String> snoozedVersion;

  public UpdatesPolicy(ConfigPairs appProps, ApplicationContext applicationContext) {
    this.appProps = appProps;
    this.applicationContext = applicationContext;
  }

  public void start(CheckForUpdatesDialog checkForUpdatesDialog) {
    this.checkForUpdatesDialog = checkForUpdatesDialog;
    this.snoozedVersion =
        new ValueAdapterPersistentPropertyImpl<>(appProps, PROP_SNOOZED_VERSION, null);

    isAutoUpdatesEnabled =
        new ModelProperty<>(
            this,
            new ValueAdapterPersistentPropertyImpl<>(appProps, PROP_IS_AUTO, null),
            PROP_IS_AUTO);
    if (isAutoUpdatesEnabled.getValue() == null) {
      isAutoUpdatesEnabled.setValueByOwner(
          UiUtils.confirmRegular(null, "prompt.doAutoUpdatesCheck", null));
    }

    if (Boolean.FALSE.equals(isAutoUpdatesEnabled.getValue())) {
      return;
    }

    if (Boolean.TRUE.equals(isAutoUpdatesEnabled.getValue())) {
      // NOTE: This will trigger check for updates and since this Pm is singleton when
      // result arrive view will just render already prepared results
      checkForUpdatesPm = applicationContext.getBean(CheckForUpdatesPm.class);
      checkForUpdatesPm.getNewVersion().addPropertyChangeListener(onNewVersionLinkChanged);
      checkForUpdatesPm.init(null, checkForUpdatesDialog.host, this);
    }
  }

  public final Action actionAutoCheckForUpdates =
      new LocalizedActionEx("action.toggleAutoCheckUpdates", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          isAutoUpdatesEnabled.setValueByOwner(!isAutoUpdatesEnabled.getValue());
        }
      };

  private final PropertyChangeListener onNewVersionLinkChanged =
      new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (!StringUtils.hasText(String.valueOf(evt.getNewValue()))) {
            return;
          }
          if (evt.getNewValue().equals(snoozedVersion.getValue())) {
            return;
          }
          checkForUpdatesDialog.actionToOpenWindow.actionPerformed(UiUtils.actionEvent(evt));
        }
      };

  public void snoozeVersion(String versionToSnooze) {
    snoozedVersion.setValue(versionToSnooze);
  }

  public ModelPropertyAccessor<Boolean> getIsAutoUpdatesEnabled() {
    return isAutoUpdatesEnabled.getModelPropertyAccessor();
  }
}
