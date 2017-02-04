package org.pgptool.gui.encryption.api;

import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyData;
import org.summerb.approaches.validation.FieldValidationException;

public interface KeyGeneratorService<TKeyData extends KeyData> {
	/**
	 * Call this method if it's anticipated that user will request key creation.
	 * Service will perform heavy mathematics in advance hopefully finishing by
	 * the time user will request key creation
	 */
	void expectNewKeyCreation();

	Key<TKeyData> createNewKey(CreateKeyParams params) throws FieldValidationException;
}
