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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
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
import org.pgptool.gui.bkgoperation.Progress;
import org.pgptool.gui.bkgoperation.Progress.Updater;
import org.pgptool.gui.bkgoperation.ProgressHandler;
import org.pgptool.gui.bkgoperation.UserReqeustedCancellationException;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.tools.IoStreamUtils;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
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
	private static final int BUFFER_SIZE = 1 << 16;

	@Override
	public void encrypt(String sourceFile, String targetFile, Collection<Key<KeyDataPgp>> recipients)
			throws UserReqeustedCancellationException {
		encrypt(sourceFile, targetFile, recipients, null);
	}

	@Override
	public String encryptText(String sourceText, Collection<Key<KeyDataPgp>> recipients) {
		try {
			PGPEncryptedDataGenerator dataGenerator = buildEncryptedDataGenerator(
					buildKeysListForEncryption(recipients));

			EncryptionSourceInfo encryptionSourceInfo = new EncryptionSourceInfo("text.asc", sourceText.length(),
					System.currentTimeMillis());
			ByteArrayOutputStream pOut = new ByteArrayOutputStream();
			ByteArrayInputStream pIn = new ByteArrayInputStream(sourceText.getBytes("UTF-8"));
			ArmoredOutputStream armoredOut = new ArmoredOutputStream(pOut);
			doEncryptFile(pIn, encryptionSourceInfo, armoredOut, dataGenerator, null, PGPLiteralData.BINARY);
			armoredOut.flush();
			armoredOut.close();
			return pOut.toString();
		} catch (Throwable t) {
			throw new RuntimeException("Encryption failed", t);
		}
	}

	@Override
	public void encrypt(String sourceFile, String targetFile, Collection<Key<KeyDataPgp>> recipients,
			ProgressHandler optionalProgressHandler) throws UserReqeustedCancellationException {
		try {
			Updater progress = null;
			if (optionalProgressHandler != null) {
				progress = Progress.create("action.encrypt", optionalProgressHandler);
				progress.updateStepInfo("encryption.progress.preparingKeys", FilenameUtils.getName(sourceFile));
			}

			PGPEncryptedDataGenerator dataGenerator = buildEncryptedDataGenerator(
					buildKeysListForEncryption(recipients));

			OutputStream out = new BufferedOutputStream(new FileOutputStream(targetFile, false));
			doEncryptFile(new FileInputStream(sourceFile), EncryptionSourceInfo.fromFile(sourceFile), out,
					dataGenerator, progress, PGPLiteralData.BINARY);
			out.close();
		} catch (Throwable t) {
			File fileToDelete = new File(targetFile);
			if (fileToDelete.exists() && !fileToDelete.delete()) {
				log.warn("Failed to delete file after failed encryption: " + targetFile);
			}
			Throwables.propagateIfInstanceOf(t, UserReqeustedCancellationException.class);
			throw new RuntimeException("Encryption failed", t);
		}
	}

	private static void doEncryptFile(InputStream pIn, EncryptionSourceInfo encryptionSourceInfo, OutputStream out,
			PGPEncryptedDataGenerator encDataGen, Updater progress, char outputType)
			throws IOException, NoSuchProviderException, PGPException, UserReqeustedCancellationException {
		OutputStream encryptedStream = encDataGen.open(out, new byte[BUFFER_SIZE]);
		PGPCompressedDataGenerator compressedDataGen = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
		OutputStream compressedStream = compressedDataGen.open(encryptedStream);
		estimateFullOperationSize(encryptionSourceInfo, progress);
		writeFileToLiteralData(pIn, encryptionSourceInfo, compressedStream, outputType, new byte[BUFFER_SIZE],
				progress);
		compressedDataGen.close();
		encryptedStream.close();
	}

	private static void estimateFullOperationSize(EncryptionSourceInfo encryptionSourceInfo, Updater progress) {
		if (progress == null) {
			return;
		}
		progress.updateTotalSteps(BigInteger.valueOf(encryptionSourceInfo.getSize()));
	}

	public static void writeFileToLiteralData(InputStream pIn, EncryptionSourceInfo encryptionSourceInfo,
			OutputStream out, char fileType, byte[] buffer, Updater progress)
			throws IOException, UserReqeustedCancellationException {
		PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
		OutputStream pOut = lData.open(out, fileType, encryptionSourceInfo.getName(),
				new Date(encryptionSourceInfo.getModifiedAt()), buffer);
		pipeFileContents(pIn, encryptionSourceInfo, pOut, buffer.length, progress);
	}

	private static void pipeFileContents(InputStream pIn, EncryptionSourceInfo encryptionSourceInfo, OutputStream pOut,
			int bufSize, Updater progress) throws IOException, UserReqeustedCancellationException {
		if (progress != null) {
			progress.updateStepInfo("encryption.progress.encrypting", encryptionSourceInfo.getName());
		}

		byte[] buf = new byte[bufSize];
		long totalRead = 0;

		int len;
		while ((len = pIn.read(buf)) > 0) {
			pOut.write(buf, 0, len);
			totalRead += len;
			updateProgress(progress, totalRead);
		}

		pOut.close();
		pIn.close();
	}

	private static void updateProgress(Updater progress, long totalBytesRead)
			throws UserReqeustedCancellationException {
		if (progress == null) {
			return;
		}
		progress.updateStepsTaken(BigInteger.valueOf(totalBytesRead));
		if (progress.isCancelationRequested()) {
			throw new UserReqeustedCancellationException();
		}
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

	@Override
	public Set<String> findKeyIdsForDecryption(String filePathName) {
		Preconditions.checkArgument(StringUtils.hasText(filePathName) && new File(filePathName).exists(),
				"filePathName has to point to existing file");
		log.debug("Looking for decryption keys for file " + filePathName);

		FileInputStream stream = null;
		try {
			stream = new FileInputStream(new File(filePathName));
		} catch (Throwable t) {
			throw new RuntimeException("Failed to open file " + filePathName, t);
		}

		try {
			return findKeyIdsForDecryption(stream);
		} finally {
			IoStreamUtils.safeClose(stream);
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Set<String> findKeyIdsForDecryption(InputStream inputStream) {
		Preconditions.checkArgument(inputStream != null, "Input stream must not be null");

		try {
			PGPObjectFactory factory = new PGPObjectFactory(PGPUtil.getDecoderStream(inputStream),
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
			throw new RuntimeException("This file doesn't look like encrypted file OR format is not supported", t);
		}
	}

	@Override
	public String decryptText(String encryptedText, PasswordDeterminedForKey<KeyDataPgp> keyAndPassword)
			throws InvalidPasswordException {
		log.debug("Decrytping text");

		Key<KeyDataPgp> decryptionKey = keyAndPassword.getMatchedKey();
		String passphrase = keyAndPassword.getPassword();

		Preconditions.checkArgument(StringUtils.hasText(encryptedText), "encryptedText required");
		Preconditions.checkArgument(decryptionKey != null, "decryption key must be provided");
		Preconditions.checkArgument(StringUtils.hasText(passphrase), "Passphrase must be provided");

		InputStream in = null;
		try {
			PGPSecretKey secretKey = decryptionKey.getKeyData().findSecretKeyById(keyAndPassword.getDecryptionKeyId());
			PGPPrivateKey privateKey = getPrivateKey(passphrase, secretKey);

			in = new ByteArrayInputStream(encryptedText.getBytes("UTF-8"));
			PGPPublicKeyEncryptedData publicKeyEncryptedData = getPublicKeyEncryptedDataByKeyId(in, secretKey);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			decryptStream(publicKeyEncryptedData, privateKey, outputStream);
			return outputStream.toString("UTF-8");
		} catch (Throwable t) {
			Throwables.propagateIfInstanceOf(t, InvalidPasswordException.class);
			log.error("Text decryption failed", t);
			throw new RuntimeException("Text decryption failed", t);
		} finally {
			IoStreamUtils.safeClose(in);
		}
	}

	@Override
	public void decrypt(String sourceFile, String targetFile, PasswordDeterminedForKey<KeyDataPgp> keyAndPassword)
			throws InvalidPasswordException {
		log.debug("Decrytping " + sourceFile);

		Key<KeyDataPgp> decryptionKey = keyAndPassword.getMatchedKey();
		String passphrase = keyAndPassword.getPassword();

		Preconditions.checkArgument(StringUtils.hasText(sourceFile) && new File(sourceFile).exists(),
				"Source file name must be correct");
		Preconditions.checkArgument(StringUtils.hasText(targetFile), "Target file name must be provided");
		Preconditions.checkArgument(decryptionKey != null, "decryption key must be provided");
		Preconditions.checkArgument(StringUtils.hasText(passphrase), "Passphrase must be provided");

		InputStream in = null;
		try {
			PGPSecretKey secretKey = decryptionKey.getKeyData().findSecretKeyById(keyAndPassword.getDecryptionKeyId());
			PGPPrivateKey privateKey = getPrivateKey(passphrase, secretKey);

			in = new BufferedInputStream(new FileInputStream(sourceFile));
			PGPPublicKeyEncryptedData publicKeyEncryptedData = getPublicKeyEncryptedDataByKeyId(in, secretKey);
			FileOutputStream outputStream = new FileOutputStream(targetFile);
			decryptStream(publicKeyEncryptedData, privateKey, outputStream);
		} catch (Throwable t) {
			Throwables.propagateIfInstanceOf(t, InvalidPasswordException.class);

			log.error("Decryption failed", t);
			throw new RuntimeException("Decryption failed", t);
		} finally {
			IoStreamUtils.safeClose(in);
		}
	}

	/**
	 * decrypt the passed in message stream.
	 * 
	 * Inspired by
	 * https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/KeyBasedFileProcessor.java
	 */
	private void decryptStream(PGPPublicKeyEncryptedData pbe, PGPPrivateKey privateKey, OutputStream outputStream) {
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
					OutputStream fOut = new BufferedOutputStream(outputStream);
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

	@Override
	public String getNameOfFileEncrypted(String encryptedFile, PasswordDeterminedForKey<KeyDataPgp> keyAndPassword)
			throws InvalidPasswordException {
		log.debug("Pre-Decrytping to get initial file name from " + encryptedFile);

		Key<KeyDataPgp> decryptionKey = keyAndPassword.getMatchedKey();
		String passphrase = keyAndPassword.getPassword();

		Preconditions.checkArgument(StringUtils.hasText(encryptedFile) && new File(encryptedFile).exists(),
				"Source file name must be correct");
		Preconditions.checkArgument(decryptionKey != null, "decryption key must be provided");
		Preconditions.checkArgument(StringUtils.hasText(passphrase), "Passphrase must be provided");

		InputStream in = null;
		try {
			PGPSecretKey secretKey = decryptionKey.getKeyData().findSecretKeyById(keyAndPassword.getDecryptionKeyId());
			PGPPrivateKey privateKey = getPrivateKey(passphrase, secretKey);

			in = new BufferedInputStream(new FileInputStream(encryptedFile));
			PGPPublicKeyEncryptedData publicKeyEncryptedData = getPublicKeyEncryptedDataByKeyId(in, secretKey);
			return getInitialFileName(publicKeyEncryptedData, privateKey);
		} catch (Throwable t) {
			Throwables.propagateIfInstanceOf(t, InvalidPasswordException.class);

			log.error("Decryption failed", t);
			throw new RuntimeException("Decryption failed", t);
		} finally {
			IoStreamUtils.safeClose(in);
		}
	}

	private String getInitialFileName(PGPPublicKeyEncryptedData pbe, PGPPrivateKey privateKey) {
		InputStream clear = null;
		try {
			clear = pbe.getDataStream(new BcPublicKeyDataDecryptorFactory(privateKey));

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
					return ld.getFileName();
				} else if (message instanceof PGPOnePassSignatureList) {
					Preconditions.checkState(pgpFactory != null, "pgpFactory supposed to be not null");
					message = pgpFactory.nextObject();
				} else if (message instanceof PGPSignatureList) {
					Preconditions.checkState(pgpFactory != null, "pgpFactory supposed to be not null");
					message = pgpFactory.nextObject();
				} else {
					throw new PGPException(
							"Don't know how to decrypt the input file. Encountered unexpected block: " + message);
				}
			}
			throw new IllegalStateException("Unknown file format, cannot determine initial file name");
		} catch (Throwable e) {
			throw new RuntimeException("Failed to get initial file name", e);
		} finally {
			IoStreamUtils.safeClose(clear);
		}
	}

}
