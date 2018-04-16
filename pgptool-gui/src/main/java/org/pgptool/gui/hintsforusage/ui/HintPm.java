package org.pgptool.gui.hintsforusage.ui;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.summerb.approaches.jdbccrud.api.dto.EntityChangedEvent;
import org.summerb.approaches.jdbccrud.common.DtoBase;

import ru.skarpushin.swingpm.base.PresentationModelBase;
import ru.skarpushin.swingpm.modelprops.ModelProperty;
import ru.skarpushin.swingpm.modelprops.ModelPropertyAccessor;
import ru.skarpushin.swingpm.tools.actions.LocalizedAction;
import ru.skarpushin.swingpm.valueadapters.ValueAdapterHolderImpl;

/**
 * 
 * NOTE: This is not actual DTO and we not going to serialize that but we use it
 * as a dirty workaround so we can use instances of this class in conjunction
 * with {@link EntityChangedEvent}
 * 
 * @author sergeyk
 *
 */
public class HintPm extends PresentationModelBase implements DtoBase {
	private static final long serialVersionUID = -6048386647066623217L;

	protected ModelProperty<String> message = new ModelProperty<>(this, new ValueAdapterHolderImpl<>(), "message");
	protected Action[] actions;

	protected HintHost hintHost;

	@SuppressWarnings("serial")
	protected Action actionClose = new LocalizedAction("action.close") {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (hintHost != null) {
				hintHost.onClose();
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

	public HintHost getHintHost() {
		return hintHost;
	}

	public void setHintHost(HintHost hintHost) {
		this.hintHost = hintHost;
	}
}
