/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2017 Sergey Karpushin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *******************************************************************************/
package org.pgptool.gui.encryption.implpgp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.util.Iterator;
import java.util.Stack;

import javax.xml.bind.ValidationException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyInfo;
import org.pgptool.gui.encryption.api.dto.KeyTypeEnum;
import org.pgptool.gui.tools.IoStreamUtils;
import org.springframework.util.StringUtils;
import org.summerb.approaches.security.api.exceptions.InvalidPasswordException;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationError;

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
		ret.setKeyType(KeyTypeEnum.KeyPair);
		PGPPublicKey key = secretKeyRing.getPublicKey();
		ret.setUser(buildUser(key.getUserIDs()));

		ret.setKeyId(KeyDataPgp.buildKeyIdStr(key.getKeyID()));
		fillDates(ret, key);
		fillAlgorithmName(ret, key);
		return ret;
	}

	@SuppressWarnings("rawtypes")
	private static String buildUser(Iterator userIDs) {
		Object ret = userIDs.next();
		return (String) ret;
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
		Stack<OutputStream> os = new Stack<>();
		try {
			os.push(new FileOutputStream(targetFilePathname));
			if ("asc".equalsIgnoreCase(FilenameUtils.getExtension(targetFilePathname))) {
				os.push(new ArmoredOutputStream(os.peek()));
			}
			key.getKeyData().getPublicKeyRing().encode(os.peek());
		} catch (Throwable t) {
			throw new RuntimeException(
					"Failed to export private key " + key.getKeyInfo().getUser() + " to " + targetFilePathname, t);
		} finally {
			while (!os.isEmpty()) {
				IoStreamUtils.safeClose(os.pop());
			}
		}
	}

	@Override
	public void exportPrivateKey(Key<KeyDataPgp> key, String targetFilePathname) {
		Preconditions.checkArgument(key != null && key.getKeyData() != null && key.getKeyInfo() != null,
				"Key must be providedand fully described");
		Preconditions.checkArgument(key.getKeyData().getSecretKeyRing() != null, "KeyPair key wasn't provided");
		Preconditions.checkArgument(StringUtils.hasText(targetFilePathname), "targetFilePathname must be provided");
		Stack<OutputStream> os = new Stack<>();
		try {
			os.push(new FileOutputStream(targetFilePathname));
			if ("asc".equalsIgnoreCase(FilenameUtils.getExtension(targetFilePathname))) {
				os.push(new ArmoredOutputStream(os.peek()));
			}
			key.getKeyData().getSecretKeyRing().encode(os.peek());
		} catch (Throwable t) {
			throw new RuntimeException(
					"Failed to export private key " + key.getKeyInfo().getUser() + " to " + targetFilePathname, t);
		} finally {
			while (!os.isEmpty()) {
				IoStreamUtils.safeClose(os.pop());
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void validateDecryptionKeyPassword(String requestedKeyId, Key<KeyDataPgp> key, String password)
			throws FieldValidationException {
		try {
			PGPSecretKey secretKey = key.getKeyData().findSecretKeyById(requestedKeyId);
			Preconditions.checkArgument(secretKey != null, "Matching secret key wasn't found");
			PGPPrivateKey privateKey = getPrivateKey(password, secretKey);
			Preconditions.checkArgument(privateKey != null, "Failed to extract private key");
		} catch (InvalidPasswordException pe) {
			throw new FieldValidationException(new ValidationError(pe.getMessageCode(), FN_PASSWORD));
		} catch (Throwable t) {
			throw new RuntimeException("Failed to verify key password", t);
		}
	}

	private PGPPrivateKey getPrivateKey(String passphrase, PGPSecretKey secretKey) throws InvalidPasswordException {
		try {
			PBESecretKeyDecryptor decryptorFactory = new BcPBESecretKeyDecryptorBuilder(
					new BcPGPDigestCalculatorProvider()).build(passphrase.toCharArray());
			PGPPrivateKey privateKey = secretKey.extractPrivateKey(decryptorFactory);
			return privateKey;
		} catch (Throwable t) {
			log.warn("Failed to extract private key. Most likely it because of incorrect passphrase provided", t);
			throw new InvalidPasswordException();
		}
	}

}
