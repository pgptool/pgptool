package org.pgptool.gui.app;

import org.summerb.approaches.i18n.HasMessageArgs;
import org.summerb.approaches.i18n.HasMessageCode;

public class Message implements HasMessageCode, HasMessageArgs {
	private String code;
	private Object[] args;

	public Message(String code, Object[] args) {
		this.code = code;
		this.args = args;
	}

	public Message(String code) {
		this.code = code;
	}

	@Override
	public Object[] getMessageArgs() {
		return args;
	}

	@Override
	public String getMessageCode() {
		return code;
	}

}
