package org.pgptool.gui.encryption.implpgp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.util.Iterator;

import javax.xml.bind.ValidationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyInfo;
import org.pgptool.gui.encryption.api.dto.KeyTypeEnum;
import org.pgptool.gui.tools.IoStreamUtils;
import org.springframework.util.StringUtils;

import com.google.common.base.Preconditions;

public class KeyFilesOperationsPgpImpl implements KeyFilesOperations<KeyDataPgp> {
	private static Logger log = Logger.getLogger(KeyFilesOperationsPgpImpl.class);

	/**
	 * Considering this as not a violation to DI since I don't see scenarios
	 * when we'll need to change this
	 */
	protected final static BcKeyFingerprintCalculator fingerprintCalculator = new BcKeyFingerprintCalculator();

	@SuppressWarnings("deprecation")
	@Override
	public Key<KeyDataPgp> readKeyFromFile(String filePathName) throws ValidationException {
		try {
			KeyDataPgp keyData = readKeyData(filePathName);

			Key<KeyDataPgp> key = new Key<>();
			key.setKeyData(keyData);
			if (keyData.getSecretKeyRing() != null) {
				key.setKeyInfo(buildKeyInfoFromSecret(keyData.getSecretKeyRing()));
			} else {
				key.setKeyInfo(buildKeyInfoFromPublic(keyData.getPublicKeyRing()));
			}
			return key;
		} catch (Throwable t) {
			throw new RuntimeException("Can't read key file", t);
		}
	}

	private KeyInfo buildKeyInfoFromPublic(PGPPublicKeyRing publicKeyRing) throws PGPException {
		KeyInfo ret = new KeyInfo();
		ret.setKeyType(KeyTypeEnum.Public);
		PGPPublicKey key = publicKeyRing.getPublicKey();
		ret.setUser(buildUser(key.getUserIDs()));

		ret.setKeyId(KeyDataPgp.buildKeyIdStr(key.getKeyID()));
		fillDates(ret, key);
		fillAlgorithmName(ret, key);
		return ret;
	}

	private static void fillDates(KeyInfo ret, PGPPublicKey key) {
		ret.setCreatedOn(new Date(key.getCreationTime().getTime()));
		if (key.getValidSeconds() != 0) {
			java.util.Date expiresAt = DateUtils.addSeconds(key.getCreationTime(), (int) key.getValidSeconds());
			ret.setExpiresAt(new Date(expiresAt.getTime()));
		}
	}

	private static void fillAlgorithmName(KeyInfo ret, PGPPublicKey key) throws PGPException {
		String alg = resolveAlgorithm(key);
		if (alg == null) {
			ret.setKeyAlgorithm("unresolved");
		} else {
			ret.setKeyAlgorithm(alg + " " + key.getBitStrength() + "bit");
		}
	}

	@SuppressWarnings("rawtypes")
	private static String resolveAlgorithm(PGPPublicKey key) throws PGPException {
		for (Iterator iter = key.getSignatures(); iter.hasNext();) {
			PGPSignature sig = (PGPSignature) iter.next();
			return PGPUtil.getSignatureName(sig.getKeyAlgorithm(), sig.getHashAlgorithm());
		}
		return null;
	}

	protected static KeyInfo buildKeyInfoFromSecret(PGPSecretKeyRing secretKeyRing) throws PGPException {
		KeyInfo ret = new KeyInfo();
		ret.setKeyType(KeyTypeEnum.Private);
		PGPPublicKey key = secretKeyRing.getPublicKey();
		ret.setUser(buildUser(key.getUserIDs()));

		ret.setKeyId(KeyDataPgp.buildKeyIdStr(key.getKeyID()));
		fillDates(ret, key);
		fillAlgorithmName(ret, key);
		return ret;
	}

	@SuppressWarnings("rawtypes")
	private static String buildUser(Iterator userIDs) {
		StringBuilder sb = new StringBuilder();
		for (; userIDs.hasNext();) {
			if (sb.length() > 0) {
				sb.append("; ");
			}
			sb.append(userIDs.next());
		}
		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	public static KeyDataPgp readKeyData(String filePathName) {
		KeyDataPgp data = new KeyDataPgp();

		try (FileInputStream stream = new FileInputStream(new File(filePathName))) {
			PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(stream), fingerprintCalculator);
			for (Iterator iter = factory.iterator(); iter.hasNext();) {
				Object section = iter.next();
				log.debug("Section found: " + section);

				if (section instanceof PGPSecretKeyRing) {
					data.setSecretKeyRing((PGPSecretKeyRing) section);
				} else if (section instanceof PGPPublicKeyRing) {
					data.setPublicKeyRing((PGPPublicKeyRing) section);
				} else {
					log.error("Unknown section enountered in a key file: " + section);
				}
			}
		} catch (Throwable t) {
			throw new RuntimeException("Error happenedd while parsing key file", t);
		}

		if (data.getPublicKeyRing() == null && data.getSecretKeyRing() == null) {
			throw new RuntimeException("Neither Secret nor Public keys were found in the input file");
		}

		return data;
	}

	@Override
	public void exportPublicKey(Key<KeyDataPgp> key, String targetFilePathname) {
		Preconditions.checkArgument(key != null && key.getKeyData() != null && key.getKeyInfo() != null,
				"Key must be providedand fully described");
		Preconditions.checkArgument(StringUtils.hasText(targetFilePathname), "targetFilePathname must be provided");
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(targetFilePathname);
			if ("asc".equalsIgnoreCase(FilenameUtils.getExtension(targetFilePathname))) {
				outputStream = new ArmoredOutputStream(outputStream);
			}
			key.getKeyData().getPublicKeyRing().encode(outputStream);
		} catch (Throwable t) {
			throw new RuntimeException(
					"Failed to export private key " + key.getKeyInfo().getUser() + " to " + targetFilePathname, t);
		} finally {
			IoStreamUtils.safeClose(outputStream);
		}
	}

	@Override
	public void exportPrivateKey(Key<KeyDataPgp> key, String targetFilePathname) {
		Preconditions.checkArgument(key != null && key.getKeyData() != null && key.getKeyInfo() != null,
				"Key must be providedand fully described");
		Preconditions.checkArgument(key.getKeyData().getSecretKeyRing() != null, "Private key wasn't provided");
		Preconditions.checkArgument(StringUtils.hasText(targetFilePathname), "targetFilePathname must be provided");
		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(targetFilePathname);
			if ("asc".equalsIgnoreCase(FilenameUtils.getExtension(targetFilePathname))) {
				outputStream = new ArmoredOutputStream(outputStream);
			}
			key.getKeyData().getSecretKeyRing().encode(outputStream);
		} catch (Throwable t) {
			throw new RuntimeException(
					"Failed to export private key " + key.getKeyInfo().getUser() + " to " + targetFilePathname, t);
		} finally {
			IoStreamUtils.safeClose(outputStream);
		}
	}

}
