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
package org.pgptool.gui.decryptedlist.impl;

import java.io.File;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.log4j.Logger;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.decryptedlist.api.DecryptedFile;
import org.pgptool.gui.decryptedlist.api.MonitoringDecryptedFilesService;
import org.pgptool.gui.tools.fileswatcher.FilesWatcherHandler;
import org.pgptool.gui.tools.fileswatcher.MultipleFilesWatcher;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.eventbus.EventBus;

public class MonitoringDecryptedFilesServiceImpl
		implements MonitoringDecryptedFilesService, InitializingBean, DisposableBean {
	private static Logger log = Logger.getLogger(MonitoringDecryptedFilesServiceImpl.class);

	protected static final long TIME_TO_ENSURE_FILE_WAS_DELETED_MS = 2000;

	private static final String PREFIX = "decrhist:";

	@Autowired
	private ConfigPairs configPairs;
	@Autowired
	private EventBus eventBus;

	private MultipleFilesWatcher multipleFilesWatcher;

	private Cache<String, DecryptedFile> recentlyRemoved = CacheBuilder.newBuilder()
			.expireAfterWrite(TIME_TO_ENSURE_FILE_WAS_DELETED_MS, TimeUnit.MILLISECONDS).build();

	@Override
	public void afterPropertiesSet() throws Exception {
		// TODO: Fix. Smells like DI violation
		multipleFilesWatcher = new MultipleFilesWatcher(dirWatcherHandler, "MonitoringDecryptedFilesService");
		List<DecryptedFile> entries = getDecryptedFiles();
		for (DecryptedFile entry : entries) {
			String decryptedFile = entry.getDecryptedFile();
			if (!new File(decryptedFile).exists()) {
				log.debug("Previously registered file is no longer exists, remove =ing it from the tracking "
						+ decryptedFile);
				remove(decryptedFile);
				continue;
			}
			multipleFilesWatcher.watchForFileChanges(decryptedFile);
		}
	}

	@Override
	public void destroy() throws Exception {
		multipleFilesWatcher.stopWatcher();
	}

	private FilesWatcherHandler dirWatcherHandler = new FilesWatcherHandler() {
		@Override
		public void handleFileChanged(Kind<?> entryDelete, String fileAbsolutePathname) {
			if (StandardWatchEventKinds.ENTRY_CREATE.equals(entryDelete)) {
				DecryptedFile recentlyRemovedEntry = recentlyRemoved.getIfPresent(fileAbsolutePathname);
				if (recentlyRemovedEntry != null) {
					add(recentlyRemovedEntry);
					return;
				}
			}

			if (!StandardWatchEventKinds.ENTRY_DELETE.equals(entryDelete)) {
				return;
			}
			remove(fileAbsolutePathname);
		}
	};

	@Override
	public synchronized void add(DecryptedFile decryptedFile) {
		try {
			Preconditions.checkArgument(decryptedFile != null, "decryptedFile is NULL");
			Preconditions.checkArgument(decryptedFile.getDecryptedFile() != null,
					"decryptedFile.DecryptedFile is NULL");
			Preconditions.checkArgument(decryptedFile.getEncryptedFile() != null,
					"decryptedFile.EncryptedFile is NULL");

			configPairs.put(buildKey(decryptedFile.getDecryptedFile()), decryptedFile);
			eventBus.post(EntityChangedEvent.added(decryptedFile));
			multipleFilesWatcher.watchForFileChanges(decryptedFile.getDecryptedFile());
		} catch (Throwable t) {
			throw new RuntimeException("Failed to register decrypted file for monitoring", t);
		}
	}

	@Override
	public synchronized void remove(String depcryptedFilePathname) {
		try {
			Preconditions.checkArgument(depcryptedFilePathname != null, "depcryptedFilePathname is NULL");

			String key = buildKey(depcryptedFilePathname);
			DecryptedFile existing = configPairs.find(key, null);
			if (existing == null) {
				// if it's not there -- nothing to delete
				return;
			}

			// Remember this file in case it will appear right away
			recentlyRemoved.put(depcryptedFilePathname, existing);

			configPairs.put(key, null);
			eventBus.post(EntityChangedEvent.removedObject(existing));
			multipleFilesWatcher.stopWatchingFile(depcryptedFilePathname);
		} catch (Throwable t) {
			throw new RuntimeException("Failed to unregister decrypted file from monitoring", t);
		}
	}

	private String buildKey(String decryptedFile) {
		return PREFIX + decryptedFile;
	}

	@Override
	public synchronized List<DecryptedFile> getDecryptedFiles() {
		return configPairs.findAllWithPrefixedKey(PREFIX);
	}

	@Override
	public DecryptedFile findByDecryptedFile(String decryptedFile) {
		return getDecryptedFiles().stream().filter(x -> x.getDecryptedFile().equals(decryptedFile)).findFirst()
				.orElse(null);
	}

	@Override
	public DecryptedFile findByEncryptedFile(String encryptedFile, Predicate<DecryptedFile> filter) {
		return getDecryptedFiles().stream().filter(x -> x.getEncryptedFile().equals(encryptedFile) && filter.test(x))
				.findFirst().orElse(null);
	}
}
