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
package org.pgptool.gui.hintsforusage.ui;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.pgptool.gui.ui.tools.swingpm.LocalizedActionEx;
import org.pgptool.gui.ui.tools.swingpm.PresentationModelBaseEx;
import org.summerb.easycrud.api.dto.EntityChangedEvent;
import org.summerb.utils.DtoBase;

import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

/**
 * 
 * NOTE: This is not actual DTO and we're not going to serialize that but we use
 * it as a dirty workaround so we can use instances of this class in conjunction
 * with {@link EntityChangedEvent}
 * 
 * @author sergeyk
 *
 */
public class HintPm extends PresentationModelBaseEx<HintHost, Void> implements DtoBase {
	private static final long serialVersionUID = -6048386647066623217L;

	protected ModelProperty<String> message = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "message");
	protected Action[] actions;

	@SuppressWarnings("serial")
	protected Action actionClose = new LocalizedActionEx("action.close", this) {
		@Override
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			if (host != null) {
				host.onClose();
			}
		}
	};

	protected Action[] getActions() {
		if (actions == null) {
			actions = buildActions();
		}
		return actions;
	}

	protected Action[] buildActions() {
		return new Action[] { actionClose };
	}

	public ModelPropertyAccessor<String> getMessage() {
		return message.getModelPropertyAccessor();
	}

	public void setMessage(String message) {
		this.message.setValueByOwner(message);
	}

	public void setHintHost(HintHost hintHost) {
		this.host = hintHost;
	}
}
