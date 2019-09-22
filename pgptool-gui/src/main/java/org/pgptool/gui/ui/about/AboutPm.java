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
package org.pgptool.gui.ui.about;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.Action;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.autoupdate.api.NewVersionChecker;
import org.pgptool.gui.autoupdate.api.UpdatePackageInfo;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ru.skarpushin.swingpm.EXPORT.base.LocalizedActionEx;
import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterReadonlyImpl;

public class AboutPm extends PresentationModelBase implements InitializingBean {
	private static Logger log = Logger.getLogger(AboutPm.class);

	@Value("${net.ts.baseUrl}")
	private String urlToSite;
	@Autowired
	private NewVersionChecker newVersionChecker;

	private ModelProperty<String> version;
	private ModelProperty<String> linkToSite;

	private ModelProperty<String> versionStatus;
	private ModelProperty<String> linkToNewVersion;

	private AboutHost host;
	private String updatePackageUrl;

	@Override
	public void afterPropertiesSet() throws Exception {
		version = new ModelProperty<String>(this,
				new ValueAdapterReadonlyImpl<String>(newVersionChecker.getCurrentVersion()), "version");
		linkToSite = new ModelProperty<String>(this, new ValueAdapterReadonlyImpl<String>(urlToSite), "linkToSite");

		versionStatus = new ModelProperty<String>(this, new ValueAdapterHolderImpl<String>(""), "versionStatus");
		linkToNewVersion = new ModelProperty<String>(this, new ValueAdapterHolderImpl<String>(""), "linkToNewVersion");
	}

	public void init(AboutHost host) {
		this.host = host;
		new Thread(checkForNewVersion).start();
	}

	private Runnable checkForNewVersion = new Runnable() {
		@Override
		public void run() {
			versionStatus.setValueByOwner(text("phrase.checkingForNewVersion"));
			try {
				UpdatePackageInfo newVersion = newVersionChecker.findNewUpdateIfAvailable();
				if (newVersion == null) {
					versionStatus.setValueByOwner(text("phrase.currentVersionIsUpToDate"));
					return;
				}

				versionStatus.setValueByOwner("");
				linkToNewVersion.setValueByOwner(text("phrase.newerVersionAvailable", newVersion.getVersion()));

				updatePackageUrl = newVersion.getUpdatePackageUrl();
			} catch (GenericException e) {
				log.warn("Failed to check for new version", e);
				versionStatus.setValueByOwner(text("phrase.currentVersionIsUpToDate"));
			}
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionClose = new LocalizedActionEx("action.close", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			host.handleClose();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionOpenSite = new LocalizedActionEx("term.linkToSite", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			try {
				Desktop.getDesktop().browse(new URI(urlToSite));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("exception.unexpected", t);
			}
			host.handleClose();
		}
	};

	@SuppressWarnings("serial")
	protected final Action actionDownloadNewVersion = new LocalizedActionEx("term.actionDownloadNewVersion", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			try {
				Desktop.getDesktop().browse(new URI(updatePackageUrl));
			} catch (Throwable t) {
				EntryPoint.reportExceptionToUser("exception.unexpected", t);
			}
			host.handleClose();
		}
	};

	public ModelPropertyAccessor<String> getVersion() {
		return version.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getLinkToSite() {
		return linkToSite.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getVersionStatus() {
		return versionStatus.getModelPropertyAccessor();
	}

	public ModelPropertyAccessor<String> getLinkToNewVersion() {
		return linkToNewVersion.getModelPropertyAccessor();
	}
}
