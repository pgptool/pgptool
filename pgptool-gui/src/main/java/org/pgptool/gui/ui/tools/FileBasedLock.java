package org.pgptool.gui.ui.tools;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import org.apache.log4j.Logger;
import org.pgptool.gui.tools.IoStreamUtils;

public class FileBasedLock implements Closeable {
	private static Logger log = Logger.getLogger(FileBasedLock.class);

	private File file;
	private RandomAccessFile randomAccessFile;
	private FileLock fileLock;

	public FileBasedLock(String file) throws FileNotFoundException {
		this.file = new File(file);
		randomAccessFile = new RandomAccessFile(this.file, "rw");
	}

	synchronized public boolean tryLock() {
		try {
			if (log.isDebugEnabled()) {
				log.debug("trylock " + file);
			}
			fileLock = randomAccessFile.getChannel().tryLock();
			boolean result = fileLock != null;
			if (log.isDebugEnabled()) {
				log.debug("trylock " + file + " result " + result);
			}
			return result;
		} catch (Throwable e) {
			log.warn("Failed to acquire lock for " + file, e);
			return false;
		}
	}

	synchronized public boolean tryLockWaitMs(long waitMs) {
		long tryUntil = System.currentTimeMillis() + waitMs;
		while (System.currentTimeMillis() < tryUntil) {
			boolean result = tryLock();
			if (result) {
				return true;
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return false;
			}
		}
		return false;
	}

	synchronized public void releaseLock() {
		try {
			if (fileLock != null) {
				if (log.isDebugEnabled()) {
					log.debug("Lock " + file + " released");
				}
				fileLock.release();
				fileLock = null;
			} else {
				if (log.isDebugEnabled()) {
					log.debug("Lock " + file + " cannot be released, because nothing to release, lock wasn't acquired");
				}
			}
		} catch (Exception e) {
			log.warn("Unable to release lock for file: " + file.getAbsolutePath(), e);
			fileLock = null;
		}
	}

	@Override
	synchronized public void close() throws IOException {
		releaseLock();
		IoStreamUtils.safeClose(randomAccessFile);
		randomAccessFile = null;
		// NOTE: Actually -- do we really need to delete this file?...
		// if (file.exists()) {
		// file.delete();
		// }
	}
}
