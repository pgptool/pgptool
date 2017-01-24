package org.pgpvault.gui.encryption.implpgp;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;

import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;
import org.pgpvault.gui.encryption.api.EncryptionService;
import org.pgpvault.gui.encryption.api.dto.Key;

import com.google.common.base.Preconditions;

/**
 * 
 * NOTE: Impl inspired by
 * https://github.com/bcgit/bc-java/blob/master/pg/src/main/java/org/bouncycastle/openpgp/examples/KeyBasedLargeFileProcessor.java
 * 
 * @author sergeyk
 *
 */
public class EncryptionServicePgpImpl implements EncryptionService<KeyDataPgp> {
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

}
