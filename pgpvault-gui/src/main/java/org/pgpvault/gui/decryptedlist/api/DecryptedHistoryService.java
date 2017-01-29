package org.pgpvault.gui.decryptedlist.api;

import java.util.List;

public interface DecryptedHistoryService {
	/**
	 * Register decrypted file for history
	 */
	void add(DecryptedFile decryptedFile);

	/**
	 * Forget about decrypted file. does NOT remove file from disk
	 * 
	 * @param depcryptedFilePathname
	 *            TODO
	 */
	void remove(String depcryptedFilePathname);

	List<DecryptedFile> getDecryptedFiles();
}
