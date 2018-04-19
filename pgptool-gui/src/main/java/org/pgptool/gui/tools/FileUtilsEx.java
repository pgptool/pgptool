package org.pgptool.gui.tools;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.bkgoperation.UserRequestedCancellationException;
import org.springframework.util.StringUtils;

public class FileUtilsEx {

	/**
	 * bruteforce filename adding index to base filename until vacant filename
	 * found.
	 */
	public static String ensureFileNameVacant(String requestedTargetFile) {
		String ret = requestedTargetFile;
		int idx = 0;
		String basePathName = FilenameUtils.getFullPath(requestedTargetFile)
				+ FilenameUtils.getBaseName(requestedTargetFile);
		String ext = FilenameUtils.getExtension(requestedTargetFile);
		while (new File(ret).exists()) {
			idx++;
			ret = basePathName + "-" + idx;
			if (StringUtils.hasText(ext)) {
				ret += "." + ext;
			}
		}
		return ret;
	}

	/**
	 * This method will pick OTHER vacant file name to write data to and whe
	 * operation is completed old file will be removed and new file will be renamed
	 * to target name.
	 * 
	 * See https://github.com/pgptool/pgptool/issues/131 "Improvement: when
	 * encrypting back - don't overwrite existing file until encryption 100%
	 * completed"
	 * 
	 * @param targetFile
	 *            desired target file name
	 * @param fileCreatorLogic
	 *            operation which will create the file
	 */
	public static void baitAndSwitch(String targetFile, FileCreatorLogic fileCreatorLogic)
			throws Exception, UserRequestedCancellationException {

		File targetFileFile = new File(targetFile);
		boolean needBaitAndSwitch = targetFileFile.exists();

		if (!needBaitAndSwitch) {
			fileCreatorLogic.createFile(targetFile);
			return;
		}

		String tempTargetFile = FileUtilsEx.ensureFileNameVacant(targetFile);
		File tempTargetFileFile = new File(tempTargetFile);
		boolean newMoved = false;
		boolean originalDeleted = false;
		try {
			fileCreatorLogic.createFile(tempTargetFile);

			FileUtils.forceDelete(targetFileFile);
			originalDeleted = true;
			FileUtils.moveFile(tempTargetFileFile, targetFileFile);
			newMoved = true;
		} catch (Throwable t) {
			if (originalDeleted && !newMoved) {
				throw new GenericException("exception.tempFileSwitchFailed", t, tempTargetFile, targetFile);
			}
			throw t;
		}
	}

	public static interface FileCreatorLogic {
		void createFile(String fileName) throws Exception, UserRequestedCancellationException;
	}
}
