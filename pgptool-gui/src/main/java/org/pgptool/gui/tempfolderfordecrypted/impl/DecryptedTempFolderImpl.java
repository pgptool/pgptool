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
package org.pgptool.gui.tempfolderfordecrypted.impl;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Random;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;
import org.pgptool.gui.app.MessageSeverity;
import org.pgptool.gui.app.Messages;
import org.pgptool.gui.config.api.ConfigsBasePathResolver;
import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.tempfolderfordecrypted.api.DecryptedTempFolder;
import org.pgptool.gui.tools.ConsoleExceptionUtils;
import org.pgptool.gui.tools.TextFile;
import org.pgptool.gui.ui.root.RootPm;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.summerb.validation.FieldValidationException;
import org.summerb.validation.ValidationError;
import org.summerb.validation.errors.FieldRequiredValidationError;

import com.google.common.base.Throwables;

public class DecryptedTempFolderImpl implements DecryptedTempFolder, InitializingBean {
	public static final String CONFIG_DECRYPTED_TEMP_FOLDER = "tempFolderForDecrypted";
	private static Logger log = Logger.getLogger(DecryptedTempFolderImpl.class);

	@Autowired
	private ConfigsBasePathResolver configsBasePathResolver;
	@Autowired
	private ConfigPairs appProps;
	@Autowired
	private RootPm rootPm;

	private String tempFolderBasePath;

	@Override
	public void afterPropertiesSet() throws Exception {
		String defaultValue1 = configsBasePathResolver.getConfigsBasePath() + File.separator + "decrypted";
		String defaultValue2 = SystemUtils.getJavaIoTmpDir().getAbsolutePath() + File.separator + "decrypted";
		tempFolderBasePath = appProps.find(CONFIG_DECRYPTED_TEMP_FOLDER, defaultValue1);

		if (ensureDirExists(tempFolderBasePath)) {
			return;
		}

		ActionEvent surrogateEvent = UiUtils.actionEvent(rootPm.findMainFrameWindow(), "ensureDirExists");
		if (ensureDirExists(defaultValue1)) {
			UiUtils.messageBox(surrogateEvent,
					Messages.get("decrypt.temp.folder.changedDueToFailure", tempFolderBasePath, defaultValue1),
					Messages.get("term.attention"), MessageSeverity.WARNING);
			setTempFolderBasePath(defaultValue1);
		} else if (ensureDirExists(defaultValue2)) {
			UiUtils.messageBox(surrogateEvent,
					Messages.get("decrypt.temp.folder.changedDueToFailure", tempFolderBasePath, defaultValue2),
					Messages.get("term.attention"), MessageSeverity.WARNING);
			setTempFolderBasePath(defaultValue2);
		} else {
			UiUtils.messageBox(surrogateEvent, Messages.get("decrypt.temp.folder.failure"),
					Messages.get("term.attention"), MessageSeverity.WARNING);
			tempFolderBasePath = "./";
		}
	}

	private boolean ensureDirExists(String dir) {
		File file = new File(dir);
		return file.exists() || file.mkdirs();
	}

	@Override
	public String getTempFolderBasePath() {
		return tempFolderBasePath;
	}

	@Override
	public void setTempFolderBasePath(String newValue) throws FieldValidationException {
		validate(newValue);
		tempFolderBasePath = newValue;
		appProps.put(CONFIG_DECRYPTED_TEMP_FOLDER, tempFolderBasePath);
	}

	private void validate(String newValue) throws FieldValidationException {
		try {
			if (!StringUtils.hasText(newValue)) {
				throw new FieldValidationException(new FieldRequiredValidationError(CONFIG_DECRYPTED_TEMP_FOLDER));
			}
			ensureDirExists(newValue);
			ensureWeCanCreateFilesThere(newValue);
		} catch (Throwable t) {
			Throwables.throwIfInstanceOf(t, FieldValidationException.class);
			log.error("Exception during validation of target folder for temp decrypted files " + newValue, t);
			throw new FieldValidationException(new ValidationError("error.temporaryFolderCannotbeUsed",
					CONFIG_DECRYPTED_TEMP_FOLDER, ConsoleExceptionUtils.getAllMessages(t)));
		}
	}

	private void ensureWeCanCreateFilesThere(String basePath) throws Exception {
		String testFile = basePath + new Random().nextInt(Integer.MAX_VALUE);
		try {
			TextFile.write(testFile, "test");
			new File(testFile).delete();
		} catch (Throwable t) {
			throw new Exception("Failed to verify if temporary folder can actually be used", t);
		}
	}

}
