/*******************************************************************************
 * PGPTool is a desktop application for pgp encryption/decryption
 * Copyright (C) 2019 Sergey Karpushin
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
 ******************************************************************************/
package org.pgptool.gui.encryption.implpgp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
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
import org.pgptool.gui.bkgoperation.Progress;
import org.pgptool.gui.bkgoperation.Progress.Updater;
import org.pgptool.gui.bkgoperation.ProgressHandler;
import org.pgptool.gui.bkgoperation.UserRequestedCancellationException;
import org.pgptool.gui.encryption.api.EncryptionService;
import org.pgptool.gui.encryption.api.InputStreamSupervisor;
import org.pgptool.gui.encryption.api.InputStreamSupervisorImpl;
import org.pgptool.gui.encryption.api.OutputStreamSupervisor;
import org.pgptool.gui.encryption.api.OutputStreamSupervisorImpl;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.tools.IoStreamUtils;
import org.pgptool.gui.ui.getkeypassword.PasswordDeterminedForKey;
import org.springframework.util.StringUtils;
import org.summerb.approaches.security.api.exceptions.InvalidPasswordException;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.io.CountingInputStream;

/**
 * 
 * NOTE: Impl inspired by
 * https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/KeyBasedLargeFileProcessor.java
 * 
 * @author Sergey Karpushin
 *
 */
public class EncryptionServicePgpImpl implements EncryptionService {
	private static Logger log = Logger.getLogger(EncryptionServicePgpImpl.class);
	private static final int BUFFER_SIZE = 1 << 16;

	@Override
	public String encryptText(String sourceText, Collection<Key> recipients) {
		try {
			PGPEncryptedDataGenerator dataGenerator = buildEncryptedDataGenerator(
					buildKeysListForEncryption(recipients));

			SourceInfo encryptionSourceInfo = new SourceInfo("text.asc", sourceText.length(),
					System.currentTimeMillis());
			ByteArrayOutputStream pOut = new ByteArrayOutputStream();
			ByteArrayInputStream pIn = new ByteArrayInputStream(sourceText.getBytes("UTF-8"));
			ArmoredOutputStream armoredOut = new ArmoredOutputStream(pOut);
			doEncryptFile(pIn, encryptionSourceInfo, armoredOut, dataGenerator, null, PGPLiteralData.BINARY);
			pIn.close();
			armoredOut.flush();
			armoredOut.close();
			return pOut.toString();
		} catch (Throwable t) {
			throw new RuntimeException("Encryption failed", t);
		}
	}

	@Override
	public void encrypt(String sourceFile, String targetFile, Collection<Key> recipients,
			ProgressHandler optionalProgressHandler, InputStreamSupervisor optionalInputStreamSupervisor,
			OutputStreamSupervisor optionalOutputStreamSupervisor) throws UserRequestedCancellationException {
		try {
			InputStreamSupervisor inputStreamSupervisor = optionalInputStreamSupervisor != null
					? optionalInputStreamSupervisor
					: new InputStreamSupervisorImpl();
			OutputStreamSupervisor outputStreamSupervisor = optionalOutputStreamSupervisor != null
					? optionalOutputStreamSupervisor
					: new OutputStreamSupervisorImpl();

			Updater progress = null;
			if (optionalProgressHandler != null) {
				progress = Progress.create("action.encrypt", optionalProgressHandler);
				progress.updateStepInfo("progress.preparingKeys");
			}

			PGPEncryptedDataGenerator dataGenerator = buildEncryptedDataGenerator(
					buildKeysListForEncryption(recipients));

			OutputStream out = new BufferedOutputStream(outputStreamSupervisor.get(targetFile));
			InputStream in = inputStreamSupervisor.get(sourceFile);
			doEncryptFile(in, SourceInfo.fromFile(sourceFile), out, dataGenerator, progress, PGPLiteralData.BINARY);
			out.close();
			in.close();
		} catch (Throwable t) {
			File fileToDelete = new File(targetFile);
			if (fileToDelete.exists() && !fileToDelete.delete()) {
				log.warn("Failed to delete file after failed encryption: " + targetFile);
			}
			Throwables.throwIfInstanceOf(t, UserRequestedCancellationException.class);
			throw new RuntimeException("Encryption failed", t);
		}
	}

	private static void doEncryptFile(InputStream pIn, SourceInfo encryptionSourceInfo, OutputStream out,
			PGPEncryptedDataGenerator encDataGen, Updater progress, char outputType)
			throws IOException, NoSuchProviderException, PGPException, UserRequestedCancellationException {
		OutputStream encryptedStream = encDataGen.open(out, new byte[BUFFER_SIZE]);
		PGPCompressedDataGenerator compressedDataGen = new PGPCompressedDataGenerator(PGPCompressedData.ZIP);
		OutputStream compressedStream = compressedDataGen.open(encryptedStream);
		estimateFullOperationSize(encryptionSourceInfo, progress);
		writeFileToLiteralData(pIn, encryptionSourceInfo, compressedStream, outputType, new byte[BUFFER_SIZE],
				progress);
		compressedDataGen.close();
		encryptedStream.close();
	}

	private static void estimateFullOperationSize(SourceInfo encryptionSourceInfo, Updater progress) {
		if (progress == null) {
			return;
		}
		progress.updateTotalSteps(BigInteger.valueOf(encryptionSourceInfo.getSize()));
	}

	public static void writeFileToLiteralData(InputStream pIn, SourceInfo encryptionSourceInfo, OutputStream out,
			char fileType, byte[] buffer, Updater progress) throws IOException, UserRequestedCancellationException {
		PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
		OutputStream pOut = lData.open(out, fileType, encryptionSourceInfo.getName(),
				new Date(encryptionSourceInfo.getModifiedAt()), buffer);
		if (progress != null) {
			progress.updateStepInfo("encryption.progress.encryptingFile", encryptionSourceInfo.getName());
		}
		pipeStream(pIn, pOut, buffer.length, progress, null);
		pOut.close();
	}

	/**
	 * @param countingStream
	 *            this stream is passed for progress reporting only. Optional, if
	 *            not provided then return from pIn method will be used
	 */
	private static void pipeStream(InputStream pIn, OutputStream pOut, int bufSize, Updater progress,
			CountingInputStream countingStream) throws IOException, UserRequestedCancellationException {
		byte[] buf = new byte[bufSize];
		long totalRead = 0;

		int len;
		while ((len = pIn.read(buf)) > 0) {
			pOut.write(buf, 0, len);

			if (countingStream == null) {
				totalRead += len;
				updateProgress(progress, totalRead);
			} else {
				updateProgress(progress, countingStream.getCount());
			}
		}
	}

	private static void updateProgress(Updater progress, long totalBytesRead)
			throws UserRequestedCancellationException {
		if (progress == null) {
			return;
		}
		progress.updateStepsTaken(BigInteger.valueOf(totalBytesRead));
		if (progress.isCancelationRequested()) {
			throw new UserRequestedCancellationException();
		}
	}

	private Collection<PGPPublicKey> buildKeysListForEncryption(Collection<Key> recipients) {
		Collection<PGPPublicKey> ret = new ArrayList<>(recipients.size());
		for (Key key : recipients) {
			PGPPublicKey encryptionKey = KeyDataPgp.get(key).findKeyForEncryption();
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
	public Set<String> findKeyIdsForDecryption(String filePathName) throws SymmetricEncryptionIsNotSupportedException {
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
	public Set<String> findKeyIdsForDecryption(InputStream inputStream)
			throws SymmetricEncryptionIsNotSupportedException {
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
						Object next = dataIter.next();
						if (next instanceof PGPPBEEncryptedData) {
							throw new SymmetricEncryptionIsNotSupportedException();
						}

						PGPPublicKeyEncryptedData data = (PGPPublicKeyEncryptedData) next;
						ret.add(KeyDataPgp.buildKeyIdStr(data.getKeyID()));
					}
					log.debug("Possible decryption with IDS: " + Arrays.toString(ret.toArray()));
					return ret;
				}
			}
			throw new RuntimeException("Information about decryption methods was not found");
		} catch (Throwable t) {
			Throwables.throwIfInstanceOf(t, SymmetricEncryptionIsNotSupportedException.class);
			throw new RuntimeException("This file doesn't look like encrypted file OR format is not supported", t);
		}
	}

	@Override
	public String decryptText(String encryptedText, PasswordDeterminedForKey keyAndPassword)
			throws InvalidPasswordException {
		log.debug("Decrytping text");

		Key decryptionKey = keyAndPassword.getMatchedKey();
		String passphrase = keyAndPassword.getPassword();

		Preconditions.checkArgument(StringUtils.hasText(encryptedText), "encryptedText required");
		Preconditions.checkArgument(decryptionKey != null, "decryption key must be provided");
		Preconditions.checkArgument(StringUtils.hasText(passphrase), "Passphrase must be provided");

		InputStream in = null;
		try {
			PGPSecretKey secretKey = KeyDataPgp.get(decryptionKey)
					.findSecretKeyById(keyAndPassword.getDecryptionKeyId());
			PGPPrivateKey privateKey = getPrivateKey(passphrase, secretKey);

			in = new ByteArrayInputStream(encryptedText.getBytes("UTF-8"));
			PGPPublicKeyEncryptedData publicKeyEncryptedData = getPublicKeyEncryptedDataByKeyId(in, secretKey);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			decryptStream(publicKeyEncryptedData, privateKey, outputStream, null, null);
			return outputStream.toString("UTF-8");
		} catch (Throwable t) {
			Throwables.throwIfInstanceOf(t, InvalidPasswordException.class);
			log.error("Text decryption failed", t);
			throw new RuntimeException("Text decryption failed", t);
		} finally {
			IoStreamUtils.safeClose(in);
		}
	}

	@Override
	public void decrypt(String sourceFile, String targetFile, PasswordDeterminedForKey keyAndPassword,
			ProgressHandler optionalProgressHandler, OutputStreamSupervisor optionalOutputStreamSupervisor)
			throws InvalidPasswordException, UserRequestedCancellationException {

		OutputStreamSupervisor outputStreamSupervisor = optionalOutputStreamSupervisor != null
				? optionalOutputStreamSupervisor
				: new OutputStreamSupervisorImpl();

		log.debug("Decrypting " + sourceFile);

		Updater progress = null;
		BigInteger sourceSize = BigInteger.valueOf(new File(sourceFile).length());
		if (optionalProgressHandler != null) {
			progress = Progress.create("action.decrypt", optionalProgressHandler);
			progress.updateStepInfo("progress.preparingKeys");
			progress.updateTotalSteps(sourceSize);
		}

		Key decryptionKey = keyAndPassword.getMatchedKey();
		String passphrase = keyAndPassword.getPassword();

		Preconditions.checkArgument(StringUtils.hasText(sourceFile) && new File(sourceFile).exists(),
				"Source file name must be correct");
		Preconditions.checkArgument(StringUtils.hasText(targetFile), "Target file name must be provided");
		Preconditions.checkArgument(decryptionKey != null, "decryption key must be provided");
		Preconditions.checkArgument(StringUtils.hasText(passphrase), "Passphrase must be provided");

		InputStream in = null;
		try {
			PGPSecretKey secretKey = KeyDataPgp.get(decryptionKey)
					.findSecretKeyById(keyAndPassword.getDecryptionKeyId());
			PGPPrivateKey privateKey = getPrivateKey(passphrase, secretKey);

			CountingInputStream countingStream = new CountingInputStream(new FileInputStream(sourceFile));
			in = new BufferedInputStream(countingStream);
			PGPPublicKeyEncryptedData publicKeyEncryptedData = getPublicKeyEncryptedDataByKeyId(in, secretKey);
			OutputStream outputStream = outputStreamSupervisor.get(targetFile);
			decryptStream(publicKeyEncryptedData, privateKey, outputStream, progress, countingStream);

			if (optionalProgressHandler != null) {
				// NOTE: The problem with decryption is that BC doesn't provide API to get
				// compressed+encrypted file size so it's hard to report progress precisely. We
				// do our best, but still it's very approximate. So that's why we need to
				// explicitly set 100% after operation was completed
				progress.updateTotalSteps(sourceSize);
			}
		} catch (Throwable t) {
			File fileToDelete = new File(targetFile);
			if (fileToDelete.exists() && !fileToDelete.delete()) {
				log.warn("Failed to delete file after failed decryption: " + targetFile);
			}

			Throwables.throwIfInstanceOf(t, InvalidPasswordException.class);
			Throwables.throwIfInstanceOf(t, UserRequestedCancellationException.class);
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
	 * 
	 * @param countingStream
	 *            this stream is passed for progress reporting only, must not be
	 *            used to actually read data
	 */
	private void decryptStream(PGPPublicKeyEncryptedData pbe, PGPPrivateKey privateKey, OutputStream outputStream,
			Updater optionalProgress, CountingInputStream countingStream) throws UserRequestedCancellationException {
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
					if (optionalProgress != null) {
						optionalProgress.updateStepInfo("progress.decrypting");
					}

					pipeStream(unc, fOut, BUFFER_SIZE, optionalProgress, countingStream);
					fOut.close();
					unc.close();

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
			Throwables.throwIfInstanceOf(e, UserRequestedCancellationException.class);
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
	public String getNameOfFileEncrypted(String encryptedFile, PasswordDeterminedForKey keyAndPassword)
			throws InvalidPasswordException {
		log.debug("Pre-Decrytping to get initial file name from " + encryptedFile);

		Key decryptionKey = keyAndPassword.getMatchedKey();
		String passphrase = keyAndPassword.getPassword();

		Preconditions.checkArgument(StringUtils.hasText(encryptedFile) && new File(encryptedFile).exists(),
				"Source file name must be correct");
		Preconditions.checkArgument(decryptionKey != null, "decryption key must be provided");
		Preconditions.checkArgument(StringUtils.hasText(passphrase), "Passphrase must be provided");

		InputStream in = null;
		try {
			PGPSecretKey secretKey = KeyDataPgp.get(decryptionKey)
					.findSecretKeyById(keyAndPassword.getDecryptionKeyId());
			PGPPrivateKey privateKey = getPrivateKey(passphrase, secretKey);

			in = new BufferedInputStream(new FileInputStream(encryptedFile));
			PGPPublicKeyEncryptedData publicKeyEncryptedData = getPublicKeyEncryptedDataByKeyId(in, secretKey);
			return getInitialFileName(publicKeyEncryptedData, privateKey);
		} catch (Throwable t) {
			Throwables.throwIfInstanceOf(t, InvalidPasswordException.class);

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
