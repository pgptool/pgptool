package org.pgptool.gui.github.api;

import java.util.List;

/**
 * DTO returned by https://api.github.com/repos/pgptool/pgptool/releases/latest
 * 
 * @author sergeyk
 *
 */
public class LatestRelease {
	private String tagName;
	private String name;
	private boolean draft;
	private boolean prerelease;
	private String publishedAt;
	private String body;

	private List<Asset> assets;

	public String getTagName() {
		return tagName;
	}

	public void setTagName(String tagName) {
		this.tagName = tagName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isDraft() {
		return draft;
	}

	public void setDraft(boolean draft) {
		this.draft = draft;
	}

	public boolean isPrerelease() {
		return prerelease;
	}

	public void setPrerelease(boolean prerelease) {
		this.prerelease = prerelease;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public List<Asset> getAssets() {
		return assets;
	}

	public void setAssets(List<Asset> assets) {
		this.assets = assets;
	}

	public String getPublishedAt() {
		return publishedAt;
	}

	public void setPublishedAt(String publishedAt) {
		this.publishedAt = publishedAt;
	}
}
