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
package org.pgptool.gui.config.impl;

import static org.pgptool.gui.config.impl.ConfigRepositoryImpl.FOLDER_NAME_CONFIGS;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.encryption.implpgp.PgpKeysRing;
import org.pgptool.gui.tools.TextFile;
import org.springframework.util.StringUtils;

public class ConfigsBasePathResolverImpl implements ConfigsBasePathResolver {
  private static final Logger log = Logger.getLogger(ConfigsBasePathResolverImpl.class);
  private String chosenLocation;
  private final String configFolderName;
  private final String customConfigBasePath;

  public ConfigsBasePathResolverImpl(String configFolderName, String customConfigBasePath) {
    this.configFolderName = configFolderName;
    this.customConfigBasePath = customConfigBasePath;
  }

  @Override
  public String getConfigsBasePath() {
    if (chosenLocation == null) {
      List<String> options = buildOptions();

      log.debug("Base path options: " + options);
      for (String option : options) {
        if (tryAccept(option)) {
          return chosenLocation;
        }
      }

      throw new IllegalStateException(
          "No config path was chosen as acceptable. Verify that the application has rights to write files on the disk");
    }

    return chosenLocation;
  }

  private List<String> buildOptions() {
    List<String> options = new LinkedList<>();
    if (StringUtils.hasText(customConfigBasePath)) {
      addOption(options, customConfigBasePath);
    }
    String userHome = SystemUtils.getUserHome().getAbsolutePath();
    String userProfile = System.getenv("USERPROFILE");

    if (SystemUtils.IS_OS_WINDOWS) {
      // Backward compatibility: if legacy configs already exist in user home, keep using it
      if (hasLegacyConfigsInUserHome(userHome)) {
        log.debug(
            "Legacy configs detected under user home, keeping user home as primary base path");
        addOption(options, userHome);
      } else {
        // Prefer Documents folder on Windows
        if (StringUtils.hasText(userProfile)) {
          addOption(options, new File(userProfile, "Documents").getAbsolutePath());
          addOption(options, new File(userProfile, "My Documents").getAbsolutePath());
        }
        // Then fallback to user home
        addOption(options, userHome);
      }

      // Additional fallbacks same as before
      addOption(options, userProfile);
      addOption(options, "~");
      return options;
    } else {
      addOption(options, userProfile);
      addOption(options, userHome);
      addOption(options, "~");
      return options;
    }
  }

  private boolean hasLegacyConfigsInUserHome(String userHomePath) {
    try {
      if (!StringUtils.hasText(userHomePath)) {
        return false;
      }
      File legacyFile =
          new File(
              new File(new File(userHomePath, configFolderName), FOLDER_NAME_CONFIGS),
              PgpKeysRing.class.getSimpleName());
      boolean exists = legacyFile.exists();
      if (exists) {
        log.debug("Detected legacy configs file: " + legacyFile.getAbsolutePath());
      }
      return exists;
    } catch (Throwable t) {
      // Any error in detection should not break resolver, treat as not found
      log.debug("Error while detecting legacy configs in user home", t);
      return false;
    }
  }

  private void addOption(List<String> options, String option) {
    if (!options.contains(option)) {
      options.add(option);
    }
  }

  private boolean tryAccept(String basePathStr) {
    log.info("Testing basePath: " + basePathStr);
    if (!StringUtils.hasText(basePathStr)) {
      return false;
    }

    File basePath = new File(basePathStr);
    if (!basePath.exists() || !basePath.isDirectory()) {
      log.info("Path is not acceptable, it does not exist or is not a directory: " + basePathStr);
      return false;
    }

    File configsFolder = new File(basePath, configFolderName);

    try {
      if (!configsFolder.exists()) {
        if (!configsFolder.mkdirs()) {
          log.info("Failed to create configs dir " + configsFolder + ", basePath is not reliable");
          return false;
        }
        File testFile = new File(configsFolder, "test.test");
        TextFile.write(testFile.getAbsolutePath(), "test");
        if (!testFile.delete()) {
          log.info("Failed to delete test file " + testFile + ", basePath is not reliable");
          return false;
        }
      }
    } catch (Throwable t) {
      log.info("Path is not acceptable, write test failed", t);
      return false;
    }

    chosenLocation = configsFolder.getAbsolutePath();
    log.info("Path was chosen as a basepath for config files: " + chosenLocation);
    return true;
  }
}
