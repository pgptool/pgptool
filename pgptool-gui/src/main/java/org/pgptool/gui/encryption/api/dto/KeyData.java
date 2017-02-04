package org.pgptool.gui.encryption.api.dto;

import java.io.Serializable;

/**
 * Placeholder abstract-class for impl-specific key data. To be sub-classed.
 * 
 * @author Sergey Karpushin
 *
 */
@SuppressWarnings("serial")
public abstract class KeyData implements Serializable {
	abstract public boolean isCanBeUsedForDecryption();

	abstract public boolean isHasAlternativeId(String alternativeId);

	abstract public boolean isCanBeUsedForEncryption();
}
