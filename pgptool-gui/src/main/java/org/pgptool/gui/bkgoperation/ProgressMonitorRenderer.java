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
package org.pgptool.gui.bkgoperation;

import static org.pgptool.gui.app.Messages.text;

import java.awt.Component;

import javax.swing.ProgressMonitor;

/**
 * Simple impl of {@link ProgressHandler} that uses standard
 * {@link ProgressMonitor} to render progress
 * 
 * @author Sergey Karpushin
 *
 */
public class ProgressMonitorRenderer implements ProgressHandler {
	private ProgressMonitor progressMonitor;
	private Component parentComponent;

	public ProgressMonitorRenderer(Component parentComponent) {
		this.parentComponent = parentComponent;
	}

	@Override
	public void onProgressUpdated(Progress progress) {
		if (progressMonitor == null) {
			progressMonitor = new ProgressMonitor(parentComponent, text(progress.getOperationCode()), "", 0, 100);
		} else {
			progressMonitor.setNote(text(progress.getStepCode(), progress.getStepArgs()));
			progressMonitor.setProgress(progress.getPercentage() == null ? 0 : progress.getPercentage());
		}

		if (progress.isCompleted()) {
			progressMonitor.close();
			progressMonitor = null;
		}
	}
}