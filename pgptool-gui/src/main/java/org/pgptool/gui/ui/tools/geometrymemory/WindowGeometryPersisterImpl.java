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
package org.pgptool.gui.ui.tools.geometrymemory;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.pgptool.gui.configpairs.api.ConfigPairs;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

public class WindowGeometryPersisterImpl extends ComponentAdapter implements WindowGeometryPersister {
	static final int MINIMUM_SANE_SIZE_VALUE = 10;

	private static final long DELAY = 500;
	private static final TimeUnit DELAY_TIME_UNIT = TimeUnit.MILLISECONDS;

	private ConfigPairs configPairs;
	private ScheduledExecutorService scheduledExecutorService;
	private Window window;
	private String keyId;

	private ScheduledFuture<?> sizeSetter;
	// private ScheduledFuture<?> locationSetter;

	/**
	 * @param scheduledExecutorService it's used to dealy interraction with
	 *                                 configPairs to avoid spamming it with values
	 *                                 while user still dragging element
	 */
	public WindowGeometryPersisterImpl(Window window, String windowId, ConfigPairs configPairs,
			ScheduledExecutorService scheduledExecutorService) {
		Preconditions.checkArgument(scheduledExecutorService != null);
		Preconditions.checkArgument(window != null);
		Preconditions.checkArgument(StringUtils.hasText(windowId));
		Preconditions.checkArgument(configPairs != null);

		this.window = window;
		// NOTE: We do + window.getMinimumSize() because from
		// version to version application windows sizes might change
		this.keyId = windowId + "_" + window.getMinimumSize();
		this.configPairs = configPairs;
		this.scheduledExecutorService = scheduledExecutorService;
		window.addComponentListener(this);
	}

	@Override
	public boolean restoreSize() {
		Preconditions.checkState(window != null, "instance was detached, can't do much after that");
		Dimension size = configPairs.find(keySize(), null);
		if (size == null) {
			return false;
		}

		window.setSize(size);
		return true;
	}

	// @Override
	// public boolean restoreLocation() {
	// Preconditions.checkState(window != null, "instance was detached, can't do
	// much after that");
	// Point location = configPairs.find(keyLocation(), null);
	// if (location == null) {
	// return false;
	// }
	//
	// window.setLocation(location);
	// return true;
	// }

	@Override
	public void componentResized(ComponentEvent e) {
		if (window == null) {
			return;
		}

		Dimension size = window.getSize();
		if (size.getWidth() < MINIMUM_SANE_SIZE_VALUE || size.getHeight() < MINIMUM_SANE_SIZE_VALUE) {
			return;
		}

		if (sizeSetter != null) {
			sizeSetter.cancel(true);
		}
		sizeSetter = scheduledExecutorService.schedule(() -> configPairs.put(keySize(), window.getSize()), DELAY,
				DELAY_TIME_UNIT);
	}

	private String keySize() {
		return keyId + "_size";
	}

	// @Override
	// public void componentMoved(ComponentEvent e) {
	// if (window == null) {
	// return;
	// }
	//
	// if (locationSetter != null) {
	// locationSetter.cancel(true);
	// }
	// locationSetter = scheduledExecutorService
	// .schedule(() -> configPairs.put(keyLocation(), window.getLocationOnScreen()),
	// DELAY, DELAY_TIME_UNIT);
	// }

	// private String keyLocation() {
	// return keyId + "_location";
	// }

	@Override
	public boolean isAttached() {
		return window != null;
	}

	@Override
	public void detach() {
		Preconditions.checkState(window != null, "instance was detached, can't do much after that");
		window.removeComponentListener(this);
		window = null;
	}
}
