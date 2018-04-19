package org.pgptool.gui.encryption.api;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

import com.google.common.base.Preconditions;

public class OutputStreamSupervisorImpl implements OutputStreamSupervisor {
	private FileOutputStream ret;

	@Override
	public OutputStream get(String fileName) throws FileNotFoundException {
		Preconditions.checkState(ret == null, "Stream was already returned");
		ret = new FileOutputStream(fileName, false);
		return ret;
	}
}
