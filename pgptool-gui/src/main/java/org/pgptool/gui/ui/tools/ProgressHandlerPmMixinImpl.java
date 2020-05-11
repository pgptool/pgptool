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
package org.pgptool.gui.ui.tools;

import org.pgptool.gui.app.Messages;
import org.pgptool.gui.bkgoperation.Progress;
import org.pgptool.gui.bkgoperation.ProgressHandler;

import ru.skarpushin.swingpm.modelprops.ModelProperty;

public class ProgressHandlerPmMixinImpl implements ProgressHandler {
	private ModelProperty<Boolean> isProgressVisible;
	private ModelProperty<Integer> progressValue;
	private ModelProperty<String> progressNote;

	public ProgressHandlerPmMixinImpl(ModelProperty<Boolean> isProgressVisible, ModelProperty<Integer> progressValue,
			ModelProperty<String> progressNote) {
		super();
		this.isProgressVisible = isProgressVisible;
		this.progressValue = progressValue;
		this.progressNote = progressNote;
	}

	@Override
	public void onProgressUpdated(Progress progress) {
		if (Thread.interrupted()) {
			progress.requestCancellation();
		}

		if (!isProgressVisible.getValue()) {
			progressNote.setValueByOwner("");
			progressValue.setValueByOwner(0);
			isProgressVisible.setValueByOwner(true);
		} else {
			int percentage = progress.getPercentage() == null ? 0 : progress.getPercentage();
			progressValue.setValueByOwner(percentage);
			if (progress.getStepCode() == null) {
				progressNote.setValueByOwner(Messages.text(progress.getOperationCode()) + " (" + percentage + "%)");
			} else {
				progressNote.setValueByOwner(
						Messages.text(progress.getStepCode(), progress.getStepArgs()) + " (" + percentage + "%)");
			}
		}

		if (progress.isCompleted()) {
			isProgressVisible.setValueByOwner(false);
		}
	}
}
