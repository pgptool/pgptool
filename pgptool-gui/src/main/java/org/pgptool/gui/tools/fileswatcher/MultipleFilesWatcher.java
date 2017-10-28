/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.pgptool.gui.tools.fileswatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.File;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.summerb.utils.threads.RecurringBackgroundTask;

public class MultipleFilesWatcher {
	private static Logger log = Logger.getLogger(MultipleFilesWatcher.class);

	private FilesWatcherHandler dirWatcherHandler;
	private String watcherName;

	private Map<WatchKey, BaseFolder> keys = new HashMap<>();
	private Map<String, BaseFolder> baseFolders = new HashMap<>();

	private Thread workerThread;
	private WatchService watcher;

	/**
	 * since WatchService is not notifying us in case of parent folders removal AND
	 * we don't want to overcomplicate code to watch ALL parent folders we're having
	 * here another simple thread that will go and check folders existance
	 */
	private RecurringBackgroundTask dirsExistanceWatcherTask;

	public MultipleFilesWatcher(FilesWatcherHandler dirWatcherHandler, String watcherName) {
		this.dirWatcherHandler = dirWatcherHandler;
		this.watcherName = watcherName;
		startWatcher();
	}

	private void startWatcher() {
		try {
			watcher = FileSystems.getDefault().newWatchService();
			workerThread = buildThreadAndStart();
			dirsExistanceWatcherTask = new RecurringBackgroundTask(dirsExistanceWatcher, 2000);
		} catch (Throwable t) {
			throw new RuntimeException("failed to install watcher", t);
		}
	}

	private Runnable dirsExistanceWatcher = new Runnable() {
		@Override
		public void run() {
			synchronized (watcherName) {
				for (BaseFolder bf : new ArrayList<>(baseFolders.values())) {
					if (new File(bf.folder).exists()) {
						continue;
					}

					for (String file : new ArrayList<>(bf.interestedFiles)) {
						String filePathName = bf.folder + File.separator + file;
						dirWatcherHandler.handleFileChanged(ENTRY_DELETE, filePathName);
					}
				}
			}
		}
	};

	public void watchForFileChanges(String filePathName) {
		try {
			String baseFolderStr = FilenameUtils.getFullPathNoEndSeparator(filePathName);
			String relativeFilename = FilenameUtils.getName(filePathName);

			synchronized (watcherName) {
				BaseFolder baseFolder = baseFolders.get(baseFolderStr);
				if (baseFolder != null) {
					log.debug("Parent directory is already being watched " + baseFolderStr
							+ ", will just add file to watch " + relativeFilename);
					baseFolder.interestedFiles.add(relativeFilename);
					return;
				}

				Path path = Paths.get(baseFolderStr);
				WatchKey key = path.register(watcher, ENTRY_DELETE, ENTRY_MODIFY, ENTRY_CREATE);
				baseFolder = new BaseFolder(baseFolderStr, key, path, relativeFilename);

				keys.put(key, baseFolder);
				baseFolders.put(baseFolderStr, baseFolder);

				log.debug("New watch key created for folder " + baseFolderStr + ", add first file " + relativeFilename);
			}
		} catch (Throwable t) {
			log.error("Failed to watch file " + filePathName, t);
			// not goinf to ruin app workflow
		}
	}

	public void stopWatchingFile(String filePathName) {
		try {
			String baseFolderStr = FilenameUtils.getFullPathNoEndSeparator(filePathName);
			String relativeFilename = FilenameUtils.getName(filePathName);

			synchronized (watcherName) {
				BaseFolder baseFolder = baseFolders.get(baseFolderStr);
				if (baseFolder == null) {
					log.debug("No associated watchers found for " + baseFolderStr);
					return;
				}

				baseFolder.interestedFiles.remove(relativeFilename);
				log.debug("File is no longer watched in folder " + baseFolderStr + " file " + relativeFilename);
				if (baseFolder.interestedFiles.size() > 0) {
					return;
				}

				// NOTE: Decided to turn this off, because file might re-appear in case it's app
				// re-created it. See #91, #75
				// keys.remove(baseFolder.key);
				// baseFolders.remove(baseFolder.folder);
				// baseFolder.key.cancel();
				// log.debug("Folder watch key is canceled " + baseFolderStr);
			}
		} catch (Throwable t) {
			log.error("Failed to watch file " + filePathName, t);
			// not goinf to ruin app workflow
		}
	}

	private Thread buildThreadAndStart() {
		Thread ret = new Thread("FilesWatcher-" + watcherName) {
			@Override
			public void run() {
				log.debug("FileWatcher thread started " + watcherName);
				boolean continueWatching = true;
				while (continueWatching) {
					WatchKey key;
					try {
						idleIfNoKeysRegistered();
						key = watcher.take();
						// NOTE: Since we're watching only one folder we assume
						// that there will be only one key for our folder
					} catch (ClosedWatchServiceException cwe) {
						log.error("ClosedWatchServiceException fired, stoppign thread.", cwe);
						return;
					} catch (InterruptedException x) {
						log.debug("FileWatcher thread stopped by InterruptedException", x);
						return;
					} catch (Throwable t) {
						log.error("Unexpected exception while checking for updates on watched file", t);
						return;
					}

					BaseFolder baseFolder = null;
					synchronized (watcherName) {
						baseFolder = keys.get(key);
					}
					if (baseFolder == null) {
						key.cancel();
						continue;
					}

					for (WatchEvent<?> event : key.pollEvents()) {
						// Context for directory entry event is the file name of
						// entry
						WatchEvent<Path> ev = cast(event);
						Path name = ev.context();
						Path child = baseFolder.path.resolve(name);
						String relativeFilename = FilenameUtils.getName(child.toString());
						if (!baseFolder.interestedFiles.contains(relativeFilename)
								&& !event.kind().equals(ENTRY_CREATE)) {
							continue;
						}

						// print out event
						log.debug("Watcher event: " + event.kind().name() + ", file " + child);
						dirWatcherHandler.handleFileChanged(event.kind(), child.toString());
					}

					// reset key and remove from set if directory no longer
					// accessible
					boolean valid = key.reset();
					if (!valid) {
						synchronized (watcherName) {
							keys.remove(key);
							baseFolders.remove(baseFolder.folder);
						}
					}
				}
				log.debug("FileWatcher thread stopped " + watcherName);
			}

			private void idleIfNoKeysRegistered() throws InterruptedException {
				while (true) {
					synchronized (watcherName) {
						if (!keys.isEmpty()) {
							break;
						}
					}
					Thread.sleep(500);
				}
			};
		};
		ret.start();
		return ret;
	}

	public void stopWatcher() {
		try {
			workerThread.interrupt();
			workerThread = null;
			watcher.close();
			watcher = null;
			dirsExistanceWatcherTask.tearDown(1000);
		} catch (Throwable t) {
			log.error("Failed to gracefully close watcher service", t);
		}
	}

	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	private static class BaseFolder {
		String folder;
		Path path;
		List<String> interestedFiles = new ArrayList<>();
		WatchKey key;

		public BaseFolder(String folder, WatchKey key, Path path, String firstFile) {
			this.folder = folder;
			this.key = key;
			this.path = path;
			interestedFiles.add(firstFile);
		}
	}
}