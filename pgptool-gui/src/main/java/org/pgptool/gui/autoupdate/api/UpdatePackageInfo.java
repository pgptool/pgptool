package org.pgptool.gui.autoupdate.api;

import java.util.Date;

public class UpdatePackageInfo {
	private String version;
	private String updatePackageUrl;
	private String title;
	private String releaseNotes;
	private Date publishedAt;

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getUpdatePackageUrl() {
		return updatePackageUrl;
	}

	public void setUpdatePackageUrl(String updatePackageUrl) {
		this.updatePackageUrl = updatePackageUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getReleaseNotes() {
		return releaseNotes;
	}

	public void setReleaseNotes(String releaseNotes) {
		this.releaseNotes = releaseNotes;
	}

	public Date getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(Date publishedAt) {
		this.publishedAt = publishedAt;
	}
}
