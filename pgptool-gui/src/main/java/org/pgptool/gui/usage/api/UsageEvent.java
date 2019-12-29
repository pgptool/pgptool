package org.pgptool.gui.usage.api;

import java.io.Serializable;

import org.summerb.utils.DtoBase;

public class UsageEvent implements DtoBase {
	private static final long serialVersionUID = -2469884370085267887L;

	/**
	 * Timestamp
	 */
	private long t;

	/**
	 * Event parameters
	 */
	private Serializable p;

	/**
	 * @deprecated do not use this manually. Empty constructor is only for IO
	 *             purposes
	 */
	@Deprecated
	public UsageEvent() {
	}

	public UsageEvent(Serializable parameters) {
		super();
		this.p = parameters;
		this.t = System.currentTimeMillis();
	}

	public long getT() {
		return t;
	}

	public void setT(long timestamp) {
		this.t = timestamp;
	}

	public Serializable getP() {
		return p;
	}

	public void setP(Serializable parameters) {
		this.p = parameters;
	}

}
