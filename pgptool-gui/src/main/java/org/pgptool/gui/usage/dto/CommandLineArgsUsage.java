package org.pgptool.gui.usage.dto;

import org.summerb.approaches.jdbccrud.common.DtoBase;

public class CommandLineArgsUsage implements DtoBase {
	private static final long serialVersionUID = -6529115848359084226L;

	private String[] args;

	public CommandLineArgsUsage() {
	}

	public CommandLineArgsUsage(String[] args) {
		super();
		this.args = args;
	}

	public String[] getArgs() {
		return args;
	}

	public void setArgs(String[] args) {
		this.args = args;
	}
}
