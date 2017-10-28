package org.pgptool.gui.github.api;

public class Asset {
	private String name;
	private String browserDownloadUrl;
	private long size;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBrowserDownloadUrl() {
		return browserDownloadUrl;
	}

	public void setBrowserDownloadUrl(String url) {
		this.browserDownloadUrl = url;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}
}
