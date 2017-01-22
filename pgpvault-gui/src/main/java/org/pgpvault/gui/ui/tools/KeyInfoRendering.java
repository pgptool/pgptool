package org.pgpvault.gui.ui.tools;

import java.sql.Date;

import org.pgpvault.gui.app.Messages;
import org.pgpvault.gui.encryption.api.dto.KeyInfo;

public class KeyInfoRendering {

	public static String keyTypeToString(KeyInfo info) {
		return Messages.get("term." + info.getKeyType().toString());
	}

	public static String dateToString(Date date) {
		return date == null ? "" : date.toString();
	}

}
