package org.pgptool.gui.encryption.api.dto;

/**
 * This DTO was created to clarify what requestedKey id was used to match key
 * from a keyring
 * 
 * @author Sergey Karpushin
 *
 * @param <TKeyData>
 */
public class MatchedKey<TKeyData extends KeyData> {
	private String requestedKeyId;
	private Key<TKeyData> matchedKey;

	/**
	 * 
	 * @param requestedKeyId
	 *            this key id found in encrypted file
	 * @param matchedKey
	 *            this key was found as a matched key, the one that contains
	 *            requested decryption key
	 */
	public MatchedKey(String requestedKeyId, Key<TKeyData> matchedKey) {
		super();
		this.requestedKeyId = requestedKeyId;
		this.matchedKey = matchedKey;
	}

	public String getRequestedKeyId() {
		return requestedKeyId;
	}

	public void setRequestedKeyId(String requestedKeyId) {
		this.requestedKeyId = requestedKeyId;
	}

	public Key<TKeyData> getMatchedKey() {
		return matchedKey;
	}

	public void setMatchedKey(Key<TKeyData> matchedKey) {
		this.matchedKey = matchedKey;
	}
}
