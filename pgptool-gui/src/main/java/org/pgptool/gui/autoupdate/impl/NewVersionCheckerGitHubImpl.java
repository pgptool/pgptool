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
package org.pgptool.gui.autoupdate.impl;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.autoupdate.api.NewVersionChecker;
import org.pgptool.gui.autoupdate.api.UpdatePackageInfo;
import org.pgptool.gui.autoupdate.impl.dto.LatestRelease;
import org.pgptool.gui.tools.HttpTools;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class NewVersionCheckerGitHubImpl implements NewVersionChecker {
	public static final String DEV_VERSION = "0.0.0.0";

	private static Logger log = Logger.getLogger(NewVersionCheckerGitHubImpl.class);

	private String configuredVersion = null;

	private String latestVersionUrl = "https://api.github.com/repos/pgptool/pgptool/releases/latest";
	private Map<String, String> headers = Collections.singletonMap("Accept", "application/vnd.github.v3+json");
	private Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

	@Override
	public UpdatePackageInfo findNewUpdateIfAvailable() throws GenericException {
		try {
			String currentVersion = getCurrentVersion();
			if (VERSION_UNRESOLVED.equals(currentVersion) || DEV_VERSION.equals(currentVersion)) {
				// not spamming github during development runs
				log.info("DEV mode detected -- not spaming github with updates check");
				return null;
			}

			String json = HttpTools.httpGet(latestVersionUrl, headers);
			LatestRelease latestRelease = gson.fromJson(json, LatestRelease.class);
			if (latestRelease.isDraft() || latestRelease.isPrerelease()) {
				log.info("Ignoring draft or prerelease release " + latestRelease.getTagName());
				return null;
			}

			if (CollectionUtils.isEmpty(latestRelease.getAssets())) {
				log.info("GitHub API returned empty array of assets for release " + latestRelease.getTagName());
				return null;
			}

			if (compareVersions(currentVersion, latestRelease.getTagName()) >= 0) {
				return null;
			}

			return buildUpdatePackageInfo(latestRelease);
		} catch (Throwable t) {
			throw new GenericException("error.failedToCheckForNewVersions", t);
		}
	}

	private UpdatePackageInfo buildUpdatePackageInfo(LatestRelease latestRelease) {
		UpdatePackageInfo ret = new UpdatePackageInfo();
		ret.setPublishedAt(parseDate(latestRelease.getPublishedAt()));
		ret.setReleaseNotes(latestRelease.getBody());
		ret.setTitle(latestRelease.getName());
		ret.setVersion(latestRelease.getTagName());
		ret.setUpdatePackageUrl(getUrlBasedOnOS(latestRelease));
		return ret;
	}

	private Date parseDate(String dateStr) {
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
			return sdf.parse(dateStr);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to parse date: " + dateStr, t);
		}
	}

	private String getUrlBasedOnOS(LatestRelease latestRelease) {
		if (isWindows()) {
			return latestRelease.getAssets().stream().filter(x -> x.getName().endsWith(".msi")).findAny()
					.orElseThrow(() -> new IllegalStateException("no *.msi asset found")).getBrowserDownloadUrl();
		}

		return latestRelease.getAssets().stream().filter(x -> x.getName().endsWith(".zip")).findAny()
				.orElseThrow(() -> new IllegalStateException("no *.zip asset found")).getBrowserDownloadUrl();
	}

	private boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("windows");
	}

	private int compareVersions(String a, String b) {
		String[] aparts = a.split("[\\.v]");
		String[] bparts = b.split("[\\.v]");
		for (int i = 0; i < Math.min(aparts.length, bparts.length); i++) {
			if (StringUtils.isEmpty(aparts[i]) || StringUtils.isEmpty(bparts[i])) {
				continue;
			}

			Integer ai = Integer.parseInt(aparts[i]);
			Integer bi = Integer.parseInt(bparts[i]);
			int result = ai.compareTo(bi);
			if (result != 0) {
				return result;
			}
		}
		return 0;
	}

	@Override
	public String getCurrentVersion() {
		try {
			if (configuredVersion != null) {
				return configuredVersion;
			}

			String ret = NewVersionCheckerGitHubImpl.class.getPackage().getImplementationVersion();
			Preconditions.checkState(StringUtils.hasText(ret),
					"ImplementationVersion cannot be resolved. Perhaps we're in the DEV mode");
			return ret;
		} catch (Throwable t) {
			log.warn("Failed to resolve current application version", t);
			return VERSION_UNRESOLVED;
		}
	}

	public static String getVerisonsInfo() {
		NewVersionChecker newVersionChecker = new NewVersionCheckerGitHubImpl();
		String pgpVersion = newVersionChecker.getCurrentVersion();
		if (NewVersionChecker.VERSION_UNRESOLVED.equals(pgpVersion)) {
			pgpVersion = DEV_VERSION;
		}

		String javaVersion = System.getProperty("java.version");
		if (javaVersion == null) {
			javaVersion = "unresolved";
		}

		return String.format(" [ PGP Tool v" + pgpVersion + ", Java v" + javaVersion + " ]");
	}

	public String getConfiguredVersion() {
		return configuredVersion;
	}

	public void setConfiguredVersion(String version) {
		this.configuredVersion = !StringUtils.hasText(version) ? null : version;
	}
}
