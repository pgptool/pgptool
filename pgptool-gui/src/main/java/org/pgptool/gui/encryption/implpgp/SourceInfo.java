package org.pgptool.gui.encryption.implpgp;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

public class SourceInfo {
	private String name;
	private long size;
	private long modifiedAt;

	public SourceInfo(String name, long size, long modifiedAt) {
		super();
		this.name = name;
		this.size = size;
		this.modifiedAt = modifiedAt;
	}

	public static SourceInfo fromFile(String filePathName) {
		File file = new File(filePathName);
		SourceInfo ret = new SourceInfo(FilenameUtils.getName(filePathName), file.length(), file.lastModified());
		return ret;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getModifiedAt() {
		return modifiedAt;
	}

	public void setModifiedAt(long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}

}
