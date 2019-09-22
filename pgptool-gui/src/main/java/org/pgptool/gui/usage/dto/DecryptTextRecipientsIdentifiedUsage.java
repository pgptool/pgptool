package org.pgptool.gui.usage.dto;

import java.io.Serializable;
import java.util.Set;

public class DecryptTextRecipientsIdentifiedUsage implements Serializable {
	private static final long serialVersionUID = -3170901342827771547L;
	private Set<String> keysIds;

	public DecryptTextRecipientsIdentifiedUsage() {
	}

	public DecryptTextRecipientsIdentifiedUsage(Set<String> keysIds) {
		this.keysIds = keysIds;
	}

	public Set<String> getKeysIds() {
		return keysIds;
	}

	public void setKeysIds(Set<String> keysIds) {
		this.keysIds = keysIds;
	}
}
