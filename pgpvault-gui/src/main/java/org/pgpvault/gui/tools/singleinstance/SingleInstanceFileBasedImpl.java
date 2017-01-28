package org.pgpvault.gui.tools.singleinstance;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.management.ManagementFactory;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.pgpvault.gui.config.impl.ConfigRepositoryImpl;
import org.pgpvault.gui.tools.dirwatcher.DirWatcherHandler;
import org.pgpvault.gui.tools.dirwatcher.SingleDirWatcher;

import com.google.common.base.Preconditions;

/**
 * This impl will create temp dir and will monitor files in this dir. Each
 * secondary instance will create file there with arguments. Primary instance
 * will watch this dear, read these files and pass arguments for processing to
 * {@link PrimaryInstanceListener}. Primary instance will hold exclusive lock on
 * a file inside this temp folder
 * 
 * @author sergeyk
 *
 */
public class SingleInstanceFileBasedImpl implements SingleInstance {
	private static Logger log = Logger.getLogger(SingleInstanceFileBasedImpl.class);

	private String tagName;
	private PrimaryInstanceListener primaryInstanceListener;

	private LockInfo lockInfo;
	private SingleDirWatcher singleDirWatcher;
	private String basePathForCommands;

	/**
	 * @param tagName
	 *            must be a valid folder name
	 */
	public SingleInstanceFileBasedImpl(String tagName) {
		this.tagName = tagName;
	}

	@Override
	public boolean tryClaimPrimaryInstanceRole(PrimaryInstanceListener primaryInstanceListener) {
		try {
			String baseTempPath = System.getProperty("java.io.tmpdir");

			File singleInstFolder = new File(baseTempPath + File.separator + tagName);
			basePathForCommands = singleInstFolder.getAbsolutePath();
			Preconditions.checkState(singleInstFolder.exists() || singleInstFolder.mkdirs(),
					"Cannot ensure sync folder for multiple instances: " + basePathForCommands);

			if (!acquireLock(basePathForCommands + File.separator + tagName + ".lock")) {
				return false;
			}

			singleDirWatcher = new SingleDirWatcher(basePathForCommands, dirWatcherHandler);
			this.primaryInstanceListener = primaryInstanceListener;
			return true;
		} catch (Throwable t) {
			// we need to release lock because apparently we cannot watch for
			// changes
			releaseLock(lockInfo);
			throw new RuntimeException("Failed to setup file watcher", t);
		}
	}

	private DirWatcherHandler dirWatcherHandler = new DirWatcherHandler() {
		@Override
		public void handleEvent(WatchEvent<?> event, Path node) {
			String fileName = node.toString();
			if (!"args".equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
				return;
			}

			try {
				InvokePrimaryInstanceArgs args = ConfigRepositoryImpl.readObject(fileName);
				primaryInstanceListener.handleArgsFromOtherInstance(args.getCommandLineArgs());
			} catch (Throwable t) {
				log.error("Failed to handle single instance command", t);
			} finally {
				if (fileName != null) {
					try {
						new File(fileName).delete();
					} catch (Throwable t) {
						log.warn("Failed to remove commands file", t);
					}
				}
			}
		}

		@Override
		public void watcherHasToStop() {
			// Not sure -- do we need to handle it somehow? If lock on file was
			// acquired this should never happen (this method should never be
			// called)
		}
	};

	private void releaseLock(LockInfo lockInfo) {
		if (lockInfo == null) {
			return;
		}
		try {
			lockInfo.fileLock.release();
			lockInfo.randomAccessFile.close();
			lockInfo.file.delete();
		} catch (Exception e) {
			log.error("Unable to remove lock file: " + lockInfo.file.getAbsolutePath(), e);
		} finally {
			lockInfo = null;
		}
	}

	private static class LockInfo {
		File file;
		RandomAccessFile randomAccessFile;
		FileLock fileLock;
	}

	private Thread shutDownHook = new Thread() {
		@Override
		public void run() {
			releaseLock(lockInfo);
			if (singleDirWatcher != null) {
				singleDirWatcher.stopWatcher();
				singleDirWatcher = null;
			}
		}
	};

	private boolean acquireLock(final String lockFile) {
		try {
			lockInfo = new LockInfo();
			lockInfo.file = new File(lockFile);
			lockInfo.randomAccessFile = new RandomAccessFile(lockInfo.file, "rw");
			lockInfo.fileLock = lockInfo.randomAccessFile.getChannel().tryLock();
			if (lockInfo.fileLock == null) {
				return false;
			}
			Runtime.getRuntime().addShutdownHook(shutDownHook);
			return true;
		} catch (Throwable e) {
			throw new RuntimeException("Failed to acquire lock file", e);
		}
	}

	@Override
	public boolean sendArgumentsToOtherInstance(String[] args) {
		if (primaryInstanceListener != null) {
			// how come?!!
			primaryInstanceListener.handleArgsFromOtherInstance(args);
			return true;
		}

		try {
			File targetFile = sendCommand(args);
			return isCommandReceived(targetFile);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to submit args", t);
		}
	}

	private boolean isCommandReceived(File targetFile) throws InterruptedException {
		long timeoutAt = System.currentTimeMillis() + 1000;
		while (targetFile.exists()) {
			Thread.sleep(50);
			if (System.currentTimeMillis() >= timeoutAt) {
				return false;
			}
		}
		return true;
	}

	private File sendCommand(String[] args) {
		String fileName = basePathForCommands + File.separator + getProcessId() + "_" + System.currentTimeMillis();
		String tempFileName = fileName + ".temp";
		ConfigRepositoryImpl.writeObject(new InvokePrimaryInstanceArgs(args), tempFileName);
		File targetFile = new File(fileName + ".args");
		new File(tempFileName).renameTo(targetFile);
		return targetFile;
	}

	private static String getProcessId() {
		String jvmName = ManagementFactory.getRuntimeMXBean().getName();
		return jvmName.split("@")[0];
	}

	@Override
	public boolean isPrimaryInstance() {
		return primaryInstanceListener != null;
	}

}
