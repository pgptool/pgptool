/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
package org.pgptool.gui.tools.dirwatcher;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import org.apache.log4j.Logger;

public class SingleDirWatcher {
  private static Logger log = Logger.getLogger(SingleDirWatcher.class);

  private String dirToWatch;
  private DirWatcherHandler dirWatcherHandler;
  private WatchService watcher;
  private Path path;
  private Thread workerThread;

  public SingleDirWatcher(String dirToWatch, DirWatcherHandler dirWatcherHandler) {
    this.dirToWatch = dirToWatch;
    this.dirWatcherHandler = dirWatcherHandler;
    startWatcher();
  }

  private void startWatcher() {
    try {
      watcher = FileSystems.getDefault().newWatchService();
      path = Paths.get(dirToWatch);
      path.register(watcher, ENTRY_CREATE);
      workerThread = buildThreadAndStart();
    } catch (Throwable t) {
      throw new RuntimeException("failed to install watcher", t);
    }
  }

  private Thread buildThreadAndStart() {
    Thread ret =
        new Thread("SingleDirWatcher:" + dirToWatch) {
          @Override
          public void run() {
            log.debug("Now watching " + dirToWatch);
            while (true) {
              WatchKey key;
              try {
                key = watcher.take();
                // NOTE: Since we're watching only one folder we assume
                // that there will be only one key for our folder
              } catch (InterruptedException x) {
                return;
              } catch (Throwable t) {
                log.error("Unexpected exception while checking for updates on watched file", t);
                return;
              }

              for (WatchEvent<?> event : key.pollEvents()) {
                // Context for directory entry event is the file name of
                // entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = path.resolve(name);

                // print out event
                log.debug("Watcher event: " + event.kind().name() + ", file " + child);
                dirWatcherHandler.handleEvent(event, child);
              }

              // reset key and remove from set if directory no longer
              // accessible
              boolean valid = key.reset();
              if (!valid) {
                dirWatcherHandler.watcherHasToStop();
                log.warn(
                    "WatchKey for " + dirToWatch + " folder is no longer valid, have to close");
                stopWatcher();
                break;
              }
            }
          }
          ;
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
    } catch (Throwable t) {
      log.error("Failed to gracefully close watcher service", t);
    }
  }

  @SuppressWarnings("unchecked")
  static <T> WatchEvent<T> cast(WatchEvent<?> event) {
    return (WatchEvent<T>) event;
  }
}
