package org.pgpvault.gui.ui.mainframe;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.decryptedlist.api.DecryptedFile;

import ru.skarpushin.swingpm.modelprops.table.LightweightTableModel;

public class DecryptedFilesModel implements LightweightTableModel<DecryptedFile> {
	public static final int COLUMN_ENCRYPTED_FILE = 0;
	public static final int COLUMN_DECRYPTED_FILE = 1;

	/**
	 * Absoilute pathname to File object
	 */
	private Map<String, File> cache = new HashMap<>();

	@Override
	public int getColumnCount() {
		return 2;
	}

	@Override
	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
		case COLUMN_ENCRYPTED_FILE:
			return Messages.get("term.encryptedFile");
		case COLUMN_DECRYPTED_FILE:
			return Messages.get("term.decryptedFile");
		default:
			throw new IllegalArgumentException("Wrong column index: " + columnIndex);
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return String.class;
	}

	@Override
	public Object getValueAt(DecryptedFile r, int columnIndex) {
		if (r == null) {
			return "";
		}

		switch (columnIndex) {
		case COLUMN_ENCRYPTED_FILE:
			return buildStringForEncryptedFile(r);
		case COLUMN_DECRYPTED_FILE:
			return r.getDecryptedFile();
		default:
			throw new IllegalArgumentException("Wrong column index: " + columnIndex);
		}
	}

	private String buildStringForEncryptedFile(DecryptedFile r) {
		boolean isExists = getOrBuildFileFor(r.getEncryptedFile()).exists();
		if (isExists) {
			return r.getEncryptedFile();
		}
		return Messages.text("term.encryptedFile.notFound") + " " + r.getEncryptedFile();
	}

	private File getOrBuildFileFor(String filePathName) {
		File ret = cache.get(filePathName);
		if (ret == null) {
			cache.put(filePathName, ret = new File(filePathName));
		}
		return ret;
	}
}
