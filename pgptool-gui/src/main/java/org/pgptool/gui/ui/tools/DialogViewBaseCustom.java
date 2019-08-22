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

import java.awt.Dialog.ModalityType;
import java.awt.Image;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JDialog;

import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.pgptool.gui.ui.tools.geometrymemory.WindowGeometryPersister;
import org.pgptool.gui.ui.tools.geometrymemory.WindowGeometryPersisterImpl;
import org.springframework.beans.factory.annotation.Autowired;

import ru.skarpushin.swingpm.EXPORT.base.DialogViewBase;
import ru.skarpushin.swingpm.base.PresentationModel;

public abstract class DialogViewBaseCustom<TPM extends PresentationModel> extends DialogViewBase<TPM> {
	@Autowired
	protected ScheduledExecutorService scheduledExecutorService;
	@Autowired
	protected ConfigPairs uiGeom;
	protected WindowGeometryPersister windowGeometryPersister;

	public static int spacing(int lettersCount) {
		return UiUtils.getFontRelativeSize(lettersCount);
	}

	@Override
	protected List<Image> getWindowIcon() {
		return WindowIcon.getWindowIcon();
	}

	@Override
	protected void showDialog() {
		super.showDialog();
		if (dialog.getModalityType() == ModalityType.MODELESS) {
			UiUtils.makeSureWindowBroughtToFront(dialog);
		}
	}

	protected void initWindowGeometryPersister(JDialog dialog, String key) {
		windowGeometryPersister = new WindowGeometryPersisterImpl(dialog, key, uiGeom, scheduledExecutorService);
		if (dialog.isResizable()) {
			if (!windowGeometryPersister.restoreSize()) {
				dialog.pack();
			}
		}
		// if (!windowGeometryPersister.restoreLocation()) {
		UiUtils.centerWindow(dialog);
		// }
	}
}
