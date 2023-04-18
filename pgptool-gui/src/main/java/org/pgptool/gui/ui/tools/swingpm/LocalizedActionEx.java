package org.pgptool.gui.ui.tools.swingpm;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.pgptool.gui.app.EntryPoint;
import org.pgptool.gui.usage.dto.ActionUsage;

import ru.skarpushin.swingpm.tools.SwingPmSettings;

public abstract class LocalizedActionEx extends AbstractAction {
	private static final long serialVersionUID = 5177364704498790332L;

	private final String actionNameMessageCode;
	private String context;

	public LocalizedActionEx(String actionNameMessageCode, Object context) {
		this.context = context instanceof String ? (String) context : context.getClass().getSimpleName();
		if (this.context.length() == 0) {
			this.context = context.getClass().getEnclosingClass().getSimpleName();
		}

		this.actionNameMessageCode = actionNameMessageCode;
	}

	@Override
	public Object getValue(String key) {
		if (Action.NAME.equals(key)) {
			return SwingPmSettings.getMessages().get(actionNameMessageCode);
		}
		return super.getValue(key);
	};

	@Override
	public void actionPerformed(ActionEvent e) {
		// I hate this part that I have to call static here. Didn't find any better
		// approach. So will have this for some time
		EntryPoint.usageLoggerStatic.write(new ActionUsage(context, actionNameMessageCode));
	}
}
