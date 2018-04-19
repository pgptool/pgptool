package org.pgptool.gui.encryption.api;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import com.google.common.base.Preconditions;

public class InputStreamSupervisorImpl implements InputStreamSupervisor {

	private FileInputStream ret;

	@Override
	public InputStream get(String fileName) throws FileNotFoundException {
		Preconditions.checkState(ret == null, "Stream was already returned");
		ret = new FileInputStream(fileName);
		return ret;
	}

}
