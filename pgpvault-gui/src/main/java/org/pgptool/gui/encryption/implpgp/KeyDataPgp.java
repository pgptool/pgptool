package org.pgptool.gui.encryption.implpgp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.util.Iterator;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.pgptool.gui.encryption.api.dto.KeyData;

/**
 * Impl of key data which stores pgp key. For one key pair there will be "ring"
 * of secret info and "ring" for public info. It's weird for naked eye.
 * 
 * @author Sergey Karpushin
 *
 */
public class KeyDataPgp extends KeyData {
	private static final long serialVersionUID = -8446784970537981225L;

	private transient PGPSecretKeyRing secretKeyRing;
	private transient PGPPublicKeyRing publicKeyRing;

	@Override
	public boolean isCanBeUsedForDecryption() {
		return secretKeyRing != null;
	}

	@Override
	public boolean isCanBeUsedForEncryption() {
		return findKeyForEncryption() != null;
	}

	public PGPPublicKey findKeyForEncryption() {
		if (getPublicKeyRing() != null) {
			return findKeyForEncryption(getPublicKeyRing().getPublicKeys());
		} else if (getSecretKeyRing() != null) {
			return findKeyForEncryption(getSecretKeyRing().getPublicKeys());
		} else {
			return null;
		}
	}

	private static PGPPublicKey findKeyForEncryption(Iterator<PGPPublicKey> publicKeys) {
		for (Iterator<PGPPublicKey> iter = publicKeys; iter.hasNext();) {
			PGPPublicKey pk = iter.next();
			if (pk.isEncryptionKey()) {
				return pk;
			}
		}
		return null;
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();

		oos.writeBoolean(secretKeyRing != null);
		if (secretKeyRing != null) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			secretKeyRing.encode(os);
			oos.writeObject(os.toByteArray());
		}

		oos.writeBoolean(publicKeyRing != null);
		if (publicKeyRing != null) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			publicKeyRing.encode(os);
			oos.writeObject(os.toByteArray()); //
		}
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();

		try {
			if (ois.readBoolean()) {
				secretKeyRing = new PGPSecretKeyRing(initInputStream(ois),
						KeyFilesOperationsPgpImpl.fingerprintCalculator);
			}
			if (ois.readBoolean()) {
				publicKeyRing = new PGPPublicKeyRing(initInputStream(ois),
						KeyFilesOperationsPgpImpl.fingerprintCalculator);
			}
		} catch (PGPException e) {
			throw new IOException("Failed to read key", e);
		}
	}

	private ByteArrayInputStream initInputStream(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		byte[] buf = (byte[]) ois.readObject();
		ByteArrayInputStream is = new ByteArrayInputStream(buf);
		return is;
	}

	public PGPSecretKeyRing getSecretKeyRing() {
		return secretKeyRing;
	}

	public void setSecretKeyRing(PGPSecretKeyRing secretKeyRing) {
		this.secretKeyRing = secretKeyRing;
	}

	public PGPPublicKeyRing getPublicKeyRing() {
		return publicKeyRing;
	}

	public void setPublicKeyRing(PGPPublicKeyRing publicKeyRing) {
		this.publicKeyRing = publicKeyRing;
	}

	public static String buildKeyIdStr(long keyID) {
		return Long.toHexString(keyID).toUpperCase();
	}

	public static long parseIdString(String hexId) {
		return new BigInteger(hexId, 16).longValue();
	}

	@Override
	public boolean isHasAlternativeId(String alternativeId) {
		long id = parseIdString(alternativeId);

		if (secretKeyRing != null) {
			return secretKeyRing.getSecretKey(id) != null;
		} else if (publicKeyRing != null) {
			return publicKeyRing.getPublicKey(id) != null;
		}
		return false;
	}

	public PGPSecretKey findSecretKeyById(String alternativeId) {
		long id = parseIdString(alternativeId);
		if (secretKeyRing != null) {
			return secretKeyRing.getSecretKey(id);
		}
		return null;
	}

}
