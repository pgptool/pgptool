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

import java.io.File;
import java.util.Arrays;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.tools.TextFile;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

public class ConfigsBasePathResolverUserHomeImpl implements ConfigsBasePathResolver {
	private static Logger log = Logger.getLogger(ConfigsBasePathResolverUserHomeImpl.class);
	private String chosenLocation;
	private String configFolderName = ".pgptool";

	public ConfigsBasePathResolverUserHomeImpl() {
	}

	@Override
	public String getConfigsBasePath() {
		if (chosenLocation == null) {
			String[] options = new String[] { System.getenv("USERPROFILE"), SystemUtils.getUserHome().getAbsolutePath(),
					"~" };
			log.debug("Base path options: " + Arrays.toString(options));

			for (int i = 0; i < options.length; i++) {
				if (tryAccept(options[i])) {
					return chosenLocation;
				}
			}

			Preconditions.checkState(false,
					"No config path was chosen as acceptable. Check application have rights to write files on the disk");
		}

		return chosenLocation;
	}

	private boolean tryAccept(String path) {
		log.debug("Testing path: " + path);
		if (!StringUtils.hasText(path)) {
			return false;
		}

		if (path.endsWith(File.separator)) {
			path = path.substring(0, path.length() - 2);
		}

		if (!new File(path).exists()) {
			return false;
		}

		path += File.separator + configFolderName;

		try {
			File tsDir = new File(path);
			if (!tsDir.exists()) {
				if (!tsDir.mkdirs()) {
					throw new RuntimeException("Failed to create configs dir " + tsDir + ", path is not reliable");
				}
				File testFile = new File(path + File.separator + "test.test");
				TextFile.write(testFile.getAbsolutePath(), "test");
				if (!testFile.delete()) {
					throw new RuntimeException("Failed to delete test file " + testFile
							+ ", this might break app logic, path is not reliable");
				}
			}
		} catch (Throwable t) {
			log.warn("Path is not acceptable, write test failed", t);
			return false;
		}

		chosenLocation = path;
		log.info("Path was chosen as a basepath for config files: " + path);
		return true;
	}

	public String getConfigFolderName() {
		return configFolderName;
	}

	public void setConfigFolderName(String configFolderName) {
		this.configFolderName = configFolderName;
	}
}
