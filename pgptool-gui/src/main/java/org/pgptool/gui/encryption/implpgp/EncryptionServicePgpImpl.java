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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPMarker;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.bc.BcPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.springframework.util.StringUtils;
import org.summerb.approaches.security.api.exceptions.InvalidPasswordException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 * 
 * NOTE: Impl inspired by
 * https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/KeyBasedLargeFileProcessor.java
 * 
 * @author Sergey Karpushin
 *
 */
public class EncryptionServicePgpImpl implements EncryptionService<KeyDataPgp> {
	private static Logger log = Logger.getLogger(EncryptionServicePgpImpl.class);

	@Override
	public void encrypt(String sourceFile, String targetFile, Collection<Key<KeyDataPgp>> recipients) {
		try {
			PGPEncryptedDataGenerator dataGenerator = buildEncryptedDataGenerator(
					buildKeysListForEncryption(recipients));

			OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile, false));
			doEncryptFile(out, sourceFile, dataGenerator);
			out.close();
		} catch (Throwable t) {
			throw new RuntimeException("Encryption failed", t);
		}
	}

	private static void doEncryptFile(OutputStream out, String fileName, PGPEncryptedDataGenerator encDataGen)
			throws IOException, NoSuchProviderException, PGPException {
		OutputStream encryptedStream = encDataGen.open(out, new byte[1 << 16]);
		PGPCompressedDataGenerator compressedDataGen = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
		PGPUtil.writeFileToLiteralData(compressedDataGen.open(encryptedStream), PGPLiteralData.BINARY,
				new File(fileName), new byte[1 << 16]);
		compressedDataGen.close();
		encryptedStream.close();
	}

	private Collection<PGPPublicKey> buildKeysListForEncryption(Collection<Key<KeyDataPgp>> recipients) {
		Collection<PGPPublicKey> ret = new ArrayList<>(recipients.size());
		for (Key<KeyDataPgp> key : recipients) {
			PGPPublicKey encryptionKey = key.getKeyData().findKeyForEncryption();
			Preconditions.checkState(encryptionKey != null,
					"Wasn't able to find encryption key for recipient " + key.getKeyInfo().getUser());
			ret.add(encryptionKey);
		}
		return ret;
	}

	private static PGPEncryptedDataGenerator buildEncryptedDataGenerator(Collection<PGPPublicKey> encKeys) {
		BcPGPDataEncryptorBuilder builder = new BcPGPDataEncryptorBuilder(PGPEncryptedData.CAST5)
				.setSecureRandom(new SecureRandom()).setWithIntegrityPacket(true);
		PGPEncryptedDataGenerator encryptedDataGenerator = new PGPEncryptedDataGenerator(builder);

		for (PGPPublicKey encKey : encKeys) {
			encryptedDataGenerator.addMethod(new BcPublicKeyKeyEncryptionMethodGenerator(encKey));
		}
		return encryptedDataGenerator;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Set<String> findKeyIdsForDecryption(String filePathName) {
		Preconditions.checkArgument(StringUtils.hasText(filePathName) && new File(filePathName).exists(),
				"filePathName has to point to existing file");

		log.debug("Looking for decryption keys for file " + filePathName);

		try (FileInputStream stream = new FileInputStream(new File(filePathName))) {
			PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(stream),
					KeyFilesOperationsPgpImpl.fingerprintCalculator);

			for (Iterator iter = factory.iterator(); iter.hasNext();) {
				Object section = iter.next();
				log.debug(section);

				if (section instanceof PGPEncryptedDataList) {
					PGPEncryptedDataList d = (PGPEncryptedDataList) section;
					HashSet<String> ret = new HashSet<>();
					for (Iterator dataIter = d.getEncryptedDataObjects(); dataIter.hasNext();) {
						PGPPublicKeyEncryptedData data = (PGPPublicKeyEncryptedData) dataIter.next();
						ret.add(KeyDataPgp.buildKeyIdStr(data.getKeyID()));
					}
					log.debug("Possible decryption with IDS: " + Arrays.toString(ret.toArray()));
					return ret;
				}
			}
			throw new RuntimeException("Information about decryption methods was not found");
		} catch (Throwable t) {
			throw new RuntimeException("This file foesnt't look like encrypted file OR format is not supported", t);
		}
	}

	@Override
	public void decrypt(String sourceFile, String targetFile, Key<KeyDataPgp> decryptionKey, String passphrase)
			throws InvalidPasswordException {
		log.debug("Decrytping " + sourceFile);

		Preconditions.checkArgument(StringUtils.hasText(sourceFile) && new File(sourceFile).exists(),
				"Source file name must be correct");
		Preconditions.checkArgument(StringUtils.hasText(targetFile), "Target file name must be provided");
		Preconditions.checkArgument(decryptionKey != null, "decryption key must be provided");
		Preconditions.checkArgument(StringUtils.hasText(passphrase), "Passphrase must be provided");

		try {
			PGPSecretKey secretKey = getSecretKey(sourceFile, decryptionKey);
			PGPPrivateKey privateKey = getPrivateKey(passphrase, secretKey);

			InputStream in = new BufferedInputStream(new FileInputStream(sourceFile));
			PGPPublicKeyEncryptedData publicKeyEncryptedData = getPublicKeyEncryptedDataByKeyId(in, secretKey);
			decryptFile(publicKeyEncryptedData, privateKey, targetFile);
			in.close();
		} catch (Throwable t) {
			Throwables.propagateIfInstanceOf(t, InvalidPasswordException.class);

			log.error("Decryption failed", t);
			throw new RuntimeException("Decryption failed", t);
		}
	}

	/**
	 * decrypt the passed in message stream.
	 * 
	 * Inspired by
	 * https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/KeyBasedFileProcessor.java
	 * 
	 * @param publicKeyEncryptedData
	 */
	private void decryptFile(PGPPublicKeyEncryptedData pbe, PGPPrivateKey privateKey, String targetFile) {
		try {
			InputStream clear = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(privateKey));

			BcPGPObjectFactory plainFact = new BcPGPObjectFactory(clear);
			Object message = plainFact.nextObject();
			if (message instanceof PGPMarker) {
				message = plainFact.nextObject();
			}

			BcPGPObjectFactory pgpFactory = null;
			if (message instanceof PGPCompressedData) {
				PGPCompressedData cData = (PGPCompressedData) message;
				pgpFactory = new BcPGPObjectFactory(cData.getDataStream());
				message = pgpFactory.nextObject();
			}

			int watchDog = 0;
			while (message != null) {
				Preconditions.checkState(watchDog++ < 100, "Inifinite loop watch dog just hit");

				if (message instanceof PGPLiteralData) {
					PGPLiteralData ld = (PGPLiteralData) message;

					// NOTE: We know initial file name (in case we need it):
					// ld.getFileName();
					InputStream unc = ld.getInputStream();
					OutputStream fOut = new BufferedOutputStream(new FileOutputStream(targetFile));
					Streams.pipeAll(unc, fOut);
					fOut.close();

					if (pbe.isIntegrityProtected()) {
						if (!pbe.verify()) {
							throw new RuntimeException("message failed integrity check");
						}
					}
					return;
				} else if (message instanceof PGPOnePassSignatureList) {
					log.info("PGPOnePassSignatureList is not implemented yet. Skipping signature validation");
					// NOTE: Here is a place to copyright from
					// http://stackoverflow.com/questions/19173181/bouncycastle-pgp-decrypt-and-verify
					Preconditions.checkArgument(pgpFactory != null,
							"File format is not supported. pgpFact is supposed to be initialized by that time");
					message = pgpFactory.nextObject();
				} else if (message instanceof PGPSignatureList) {
					log.info("PGPSignatureList is not implemented yet. Skipping signature validation");
					Preconditions.checkArgument(pgpFactory != null,
							"File format is not supported. pgpFact is supposed to be initialized by that time");
					message = pgpFactory.nextObject();
				} else {
					throw new PGPException(
							"Don't know how to decrypt the input file. Encountered unexpected block: " + message);
				}
			}
		} catch (Throwable e) {
			throw new RuntimeException("Decryption failed", e);
		}
	}

	@SuppressWarnings("rawtypes")
	private PGPPublicKeyEncryptedData getPublicKeyEncryptedDataByKeyId(InputStream in, PGPSecretKey secretKey) {
		try {
			PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(in),
					KeyFilesOperationsPgpImpl.fingerprintCalculator);

			for (Iterator iter = factory.iterator(); iter.hasNext();) {
				Object section = iter.next();
				if (section instanceof PGPEncryptedDataList) {
					PGPEncryptedDataList d = (PGPEncryptedDataList) section;
					for (Iterator dataIter = d.getEncryptedDataObjects(); dataIter.hasNext();) {
						PGPPublicKeyEncryptedData data = (PGPPublicKeyEncryptedData) dataIter.next();
						if (data.getKeyID() == secretKey.getKeyID()) {
							return data;
						}
					}
				}
			}
			// NOTE: That is actually should NEVER happen since secret key we're
			// supposed to use here was taken exactly same way as we're looking
			// for PGPPublicKeyEncryptedData now
			throw new RuntimeException("Encryption data matching given key "
					+ KeyDataPgp.buildKeyIdStr(secretKey.getKeyID()) + " wasn't found");
		} catch (Throwable t) {
			throw new RuntimeException("Failed to find Encryption data section in encrypted file", t);
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

	private PGPSecretKey getSecretKey(String sourceFile, Key<KeyDataPgp> decryptionKey) {
		try {
			Set<String> possibleDecryptionKeys = findKeyIdsForDecryption(sourceFile);
			for (String requestedDecryptionKeyId : possibleDecryptionKeys) {
				PGPSecretKey secretKey = decryptionKey.getKeyData().findSecretKeyById(requestedDecryptionKeyId);
				if (secretKey != null) {
					return secretKey;
				}
			}
			throw new IllegalArgumentException(
					"The key that was selected is actually not suitable for decryption of given source file");
		} catch (Throwable t) {
			throw new RuntimeException("Cannot resolve secret key for decryption", t);
		}
	}

}
