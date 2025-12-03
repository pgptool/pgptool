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

import static org.pgptool.gui.app.Messages.text;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import org.pgptool.gui.autoupdate.api.NewVersionChecker;
import org.pgptool.gui.autoupdate.api.UpdatePackageInfo;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.ui.tools.UrlOpener;
import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class CheckForUpdatesPm extends PresentationModelBaseEx<CheckForUpdatesHost, UpdatesPolicy>
    implements InitializingBean {
  private static final Logger log = LoggerFactory.getLogger(CheckForUpdatesPm.class);

  private final NewVersionChecker newVersionChecker;

  private ModelProperty<String> currentVersion;
  private ModelProperty<String> versionCheckStatus;
  private ModelProperty<String> linkToNewVersion;
  private ModelProperty<String> newVersion;
  private ModelProperty<String> newVersionTitle;
  private ModelProperty<String> newVersionReleaseNotes;

  private String updatePackageUrl;

  public CheckForUpdatesPm(NewVersionChecker newVersionChecker) {
    this.newVersionChecker = newVersionChecker;
  }

  @Override
  public void afterPropertiesSet() {
    currentVersion =
        new ModelProperty<>(
            this, new ValueAdapterReadonlyImpl<>(newVersionChecker.getCurrentVersion()), "version");

    versionCheckStatus =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "versionStatus");
    linkToNewVersion =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "linkToNewVersion");

    newVersionTitle =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "newVersionTitle");
    newVersion = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "newVersionId");
    newVersionReleaseNotes =
        new ModelProperty<>(this, new ValueAdapterHolderImpl<>(""), "newVersionReleaseNotes");
  }

  @Override
  public boolean init(
      ActionEvent originAction, CheckForUpdatesHost host, UpdatesPolicy updatesPolicy) {
    super.init(originAction, host, updatesPolicy);
    actionSnoozeVersion.setEnabled(false);
    actionDownloadNewVersion.setEnabled(false);
    new Thread(checkForNewVersion).start();
    return true;
  }

  private final Runnable checkForNewVersion =
      new Runnable() {
        @Override
        public void run() {
          versionCheckStatus.setValueByOwner(text("phrase.checkingForNewVersion"));
          try {
            UpdatePackageInfo newVersionInfo = newVersionChecker.findNewUpdateIfAvailable();
            if (newVersionInfo == null) {
              versionCheckStatus.setValueByOwner(text("phrase.currentVersionIsUpToDate"));
              newVersionReleaseNotes.setValueByOwner(text("phrase.newVersionWasntPublishedYet"));
              return;
            }

            versionCheckStatus.setValueByOwner("");
            newVersion.setValueByOwner(newVersionInfo.getVersion());
            actionSnoozeVersion.setEnabled(true);
            actionDownloadNewVersion.setEnabled(true);
            linkToNewVersion.setValueByOwner(
                text("phrase.newerVersionAvailable", newVersionInfo.getVersion()));
            newVersionTitle.setValueByOwner(newVersionInfo.getTitle());
            newVersionReleaseNotes.setValueByOwner(newVersionInfo.getReleaseNotes());
            updatePackageUrl = newVersionInfo.getUpdatePackageUrl();
          } catch (Throwable e) {
            log.warn("Failed to check for new version", e);
            versionCheckStatus.setValueByOwner("");
            newVersionReleaseNotes.setValueByOwner(ConsoleExceptionUtils.getAllMessages(e));
          }
        }
      };

  protected final Action actionClose =
      new LocalizedActionEx("action.close", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          host.handleClose();
        }
      };

  protected final Action actionSnoozeVersion =
      new LocalizedActionEx("action.snoozeVersion", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          initParams.snoozeVersion(newVersion.getValue());
          host.handleClose();
        }
      };

  protected final Action actionDownloadNewVersion =
      new LocalizedActionEx("action.actionDownloadNewVersion", this) {
        @Override
        public void actionPerformed(ActionEvent e) {
          super.actionPerformed(e);
          UrlOpener.open(e, updatePackageUrl);
          host.handleClose();
        }
      };

  public ModelPropertyAccessor<String> getCurrentVersion() {
    return currentVersion.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getVersionCheckStatus() {
    return versionCheckStatus.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getLinkToNewVersion() {
    return linkToNewVersion.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getNewVersionTitle() {
    return newVersionTitle.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getNewVersionReleaseNotes() {
    return newVersionReleaseNotes.getModelPropertyAccessor();
  }

  public ModelPropertyAccessor<String> getNewVersion() {
    return newVersion.getModelPropertyAccessor();
  }
}
