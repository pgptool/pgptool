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
package org.pgptool.gui.ui.tools;

import java.awt.Image;
import java.util.List;

import ru.skarpushin.swingpm.base.DialogViewBase;
import ru.skarpushin.swingpm.base.PresentationModel;

public abstract class DialogViewBaseCustom<TPM extends PresentationModel> extends DialogViewBase<TPM> {
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
		UiUtils.makeSureWindowBroughtToFront(dialog);
	}
}
