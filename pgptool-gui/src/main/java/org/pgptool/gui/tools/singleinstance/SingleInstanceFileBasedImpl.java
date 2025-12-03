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
package org.pgptool.gui.tools.singleinstance;

import com.google.common.base.Preconditions;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import org.apache.commons.io.FilenameUtils;
import org.pgptool.gui.config.impl.ConfigRepositoryImpl;
import org.pgptool.gui.tools.IoStreamUtils;
import org.pgptool.gui.tools.dirwatcher.DirWatcherHandler;
import org.pgptool.gui.tools.dirwatcher.SingleDirWatcher;
import org.pgptool.gui.ui.tools.FileBasedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This impl will create temp dir and will monitor files in this dir. Each secondary instance will
 * create file there with arguments. Primary instance will watch this dear, read these files and
 * pass arguments for processing to {@link PrimaryInstanceListener}. Primary instance will hold
 * exclusive lock on a file inside this temp folder
 *
 * @author Sergey Karpushin
 */
public class SingleInstanceFileBasedImpl implements SingleInstance {
  private static final Logger log = LoggerFactory.getLogger(SingleInstanceFileBasedImpl.class);

  private static final int LOCK_ARGS_SUBMISSION_TIMEOUT = 3000;
  private static final String ROLE_LOCK_FILE_EXTENSION = ".role-lock";
  private static final String DIR_LOCK_FILE_EXTENSION = ".dir-lock";
  private static final String PARAMS_FILE_EXTENSION = "args";
  private static final String PARAMS_FILE_EXTENSION_TEMP = "temp";

  private final String tagName;
  private PrimaryInstanceListener primaryInstanceListener;

  private FileBasedLock lockRole;

  /**
   * This additional lock will be used by parties to enter critical section before writing any
   * changes to this directory. It will help us avoid any race conditions
   */
  private final FileBasedLock lockNewArgsSubmissons;

  private SingleDirWatcher singleDirWatcher;
  private final String basePathForCommands;

  /**
   * @param tagName must be a valid folder name
   */
  public SingleInstanceFileBasedImpl(String tagName) {
    this.tagName = tagName;
    String baseTempPath = System.getProperty("java.io.tmpdir");
    basePathForCommands = getDirForSingleInstance(baseTempPath);

    try {
      lockNewArgsSubmissons =
          new FileBasedLock(
              basePathForCommands + File.separator + tagName + DIR_LOCK_FILE_EXTENSION);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to init lock file object", t);
    }
  }

  @Override
  public boolean tryClaimPrimaryInstanceRole(PrimaryInstanceListener primaryInstanceListener) {
    try {
      lockNewArgsSubmissons.tryLockWaitMs(LOCK_ARGS_SUBMISSION_TIMEOUT);

      lockRole =
          new FileBasedLock(
              basePathForCommands + File.separator + tagName + ROLE_LOCK_FILE_EXTENSION);
      if (!lockRole.tryLock()) {
        log.info("This instance is not condiered as a primary instance");
        return false;
      }
      log.info("From now on this instance considered as a primary instance");

      Runtime.getRuntime().addShutdownHook(shutDownHook);

      this.primaryInstanceListener = primaryInstanceListener;
      singleDirWatcher = new SingleDirWatcher(basePathForCommands, dirWatcherHandler);
      return true;
    } catch (Throwable t) {
      // we need to release lock because apparently we cannot watch for
      // changes
      IoStreamUtils.safeClose(lockRole);
      throw new RuntimeException("Failed to setup file watcher", t);
    } finally {
      lockNewArgsSubmissons.releaseLock();
    }
  }

  private String getDirForSingleInstance(String baseTempPath) {
    File singleInstFolder = new File(baseTempPath + File.separator + tagName);
    String ret = singleInstFolder.getAbsolutePath();
    Preconditions.checkState(
        singleInstFolder.exists() || singleInstFolder.mkdirs(),
        "Cannot ensure sync folder for multiple instances: " + ret);
    return ret;
  }

  private final DirWatcherHandler dirWatcherHandler =
      new DirWatcherHandler() {
        @Override
        public void handleEvent(WatchEvent<?> event, Path node) {
          String fileName = node.toString();
          if (!PARAMS_FILE_EXTENSION.equalsIgnoreCase(FilenameUtils.getExtension(fileName))) {
            return;
          }

          try {
            InvokePrimaryInstanceArgs args = tryReadArgs(fileName);
            Preconditions.checkState(args != null, "Failed to read args file");
            primaryInstanceListener.handleArgsFromOtherInstance(args.getCommandLineArgs());
            safeDelete(fileName);
          } catch (Throwable t) {
            log.error("Failed to handle single instance command", t);
          }
        }

        private void safeDelete(String fileName) {
          try {
            new File(fileName).delete();
          } catch (Throwable t) {
            log.warn("Failed to remove commands file", t);
          }
        }

        /**
         * We might need to perform couple attempts because rename action initiated by other
         * instance might block args file
         */
        private InvokePrimaryInstanceArgs tryReadArgs(String fileName) throws InterruptedException {
          long timeoutAt = System.currentTimeMillis() + LOCK_ARGS_SUBMISSION_TIMEOUT;
          InvokePrimaryInstanceArgs args = ConfigRepositoryImpl.readObject(fileName);
          while (args == null && System.currentTimeMillis() < timeoutAt) {
            Thread.sleep(50);
            args = ConfigRepositoryImpl.readObject(fileName);
          }
          return args;
        }

        @Override
        public void watcherHasToStop() {
          // Not sure -- do we need to handle it somehow? If lock on file was
          // acquired this should never happen (this method should never be
          // called)
        }
      };

  private final Thread shutDownHook =
      new Thread() {
        @Override
        public void run() {
          IoStreamUtils.safeClose(lockRole);
          if (singleDirWatcher != null) {
            singleDirWatcher.stopWatcher();
            singleDirWatcher = null;
          }
        }
      };

  @Override
  public boolean sendArgumentsToOtherInstance(String[] args) {
    if (primaryInstanceListener != null) {
      // how come?!!
      primaryInstanceListener.handleArgsFromOtherInstance(args);
      return true;
    }

    try {
      if (!lockNewArgsSubmissons.tryLockWaitMs(LOCK_ARGS_SUBMISSION_TIMEOUT)) {
        return false;
      }
      File targetFile = sendCommand(args);
      return isCommandReceived(targetFile);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to submit args", t);
    } finally {
      lockNewArgsSubmissons.releaseLock();
    }
  }

  private boolean isCommandReceived(File targetFile) throws InterruptedException {
    long timeoutAt = System.currentTimeMillis() + LOCK_ARGS_SUBMISSION_TIMEOUT;
    while (targetFile.exists()) {
      Thread.sleep(50);
      if (System.currentTimeMillis() >= timeoutAt) {
        log.warn(
            "As a secondary instance we can't see confirmation that our args were received {}",
            targetFile);
        return false;
      }
    }
    log.info("As a secondary we see args were processed by primary instance {}", targetFile);
    return true;
  }

  private File sendCommand(String[] args) {
    String fileName =
        basePathForCommands + File.separator + getProcessId() + "_" + System.currentTimeMillis();
    String tempFileName = fileName + "." + PARAMS_FILE_EXTENSION_TEMP;
    log.debug("Creating temp args file: {}", tempFileName);
    ConfigRepositoryImpl.writeObject(new InvokePrimaryInstanceArgs(args), tempFileName);
    log.debug("Done creating temp args file: {}", tempFileName);
    File targetFile = new File(fileName + "." + PARAMS_FILE_EXTENSION);
    log.debug("Renaming temp args file: {}", tempFileName);
    new File(tempFileName).renameTo(targetFile);
    log.debug("Done renaming temp args file: {}", tempFileName);
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
