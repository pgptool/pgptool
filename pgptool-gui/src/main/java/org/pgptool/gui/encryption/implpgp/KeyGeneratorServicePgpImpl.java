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

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.crypto.spec.DHParameterSpec;

import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class KeyGeneratorServicePgpImpl implements KeyGeneratorService {
	private static final Logger log = Logger.getLogger(KeyGeneratorServicePgpImpl.class);
	private static final String PROVIDER = "BC";

	@Autowired
	private ExecutorService executorService;

	// Master key params
	private String masterKeyAlgorithm;
	private String masterKeyPurpose;
	private int masterKeySize;
	private KeyPairParams masterKeyParameters;
	private String masterKeySignerAlgorithm;
	private String masterKeySignerHashingAlgorithm;

	// Secret Key encryption
	private String secretKeyHashingAlgorithm;
	private String secretKeyEncryptionAlgorithm;

	// Encryption key params
	private String encryptionKeyAlgorithm;
	private String encryptionKeyPurpose;
	private BigInteger dhParamsPrimeModulus;
	private BigInteger dhParamsBaseGenerator;

	private Map<KeyPairParams, Future<KeyPair>> pregeneratedKeyPairs = new ConcurrentHashMap<>();

	public KeyGeneratorServicePgpImpl() {
		KeyRingServicePgpImpl.touch();
	}

	@Override
	public Key createNewKey(CreateKeyParams params) throws FieldValidationException {
		try {
			Preconditions.checkArgument(params != null, "params must not be null");
			assertParamsValid(params);

			// Create Master key
			KeyPair masterKey = getOrGenerateKeyPair(getMasterKeyParameters());
			PGPKeyPair masterKeyBc = new JcaPGPKeyPair(algorithmNameToTag(masterKeyPurpose), masterKey, new Date());
			BcPGPContentSignerBuilder keySignerBuilderBc = new BcPGPContentSignerBuilder(
					algorithmNameToTag(masterKeyPurpose), hashAlgorithmNameToTag(masterKeySignerHashingAlgorithm));

			// Setup seret key encryption
			PGPDigestCalculator digestCalc = new BcPGPDigestCalculatorProvider()
					.get(hashAlgorithmNameToTag(secretKeyHashingAlgorithm));
			BcPBESecretKeyEncryptorBuilder encryptorBuilderBC = new BcPBESecretKeyEncryptorBuilder(
					symmetricKeyAlgorithmNameToTag(secretKeyEncryptionAlgorithm), digestCalc);
			PBESecretKeyEncryptor keyEncryptorBc = encryptorBuilderBC.build(params.getPassphrase().toCharArray());

			// Key pair generator
			String userName = params.getFullName() + " <" + params.getEmail() + ">";
			PGPKeyRingGenerator keyPairGeneratorBc = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
					masterKeyBc, userName, digestCalc, null, null, keySignerBuilderBc, keyEncryptorBc);

			// Add Sub-key for encryption
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(encryptionKeyAlgorithm, PROVIDER);
			if ("ELGAMAL".equals(encryptionKeyAlgorithm)) {
				keyPairGenerator.initialize(new DHParameterSpec(dhParamsPrimeModulus, dhParamsBaseGenerator));
			} else if ("RSA".equals(encryptionKeyAlgorithm)) {
				// Re-using master key size.
				keyPairGenerator.initialize(new RSAKeyGenParameterSpec(masterKeySize, RSAKeyGenParameterSpec.F4));
			} else {
				throw new IllegalArgumentException(
						"Hanlding of parameter creation for " + encryptionKeyAlgorithm + " is not implemented");
			}
			KeyPair encryptionSubKey = keyPairGenerator.generateKeyPair();
			PGPKeyPair encryptionSubKeyBc = new JcaPGPKeyPair(algorithmNameToTag(encryptionKeyPurpose),
					encryptionSubKey, new Date());
			keyPairGeneratorBc.addSubKey(encryptionSubKeyBc);

			// TBD-191: Also add a sub-key for signing
			// KeyPair signatureSubKey = keyPairGenerator.generateKeyPair();
			// PGPKeyPair signatureSubKeyBc = new
			// TBD-191: RSA_SIGN must not be hardcoded
			// JcaPGPKeyPair(algorithmNameToTag("RSA_SIGN"), signatureSubKey,
			// new Date());
			// keyPairGeneratorBc.addSubKey(signatureSubKeyBc);

			// building ret
			return buildKey(keyPairGeneratorBc);
		} catch (Throwable t) {
			Throwables.throwIfInstanceOf(t, FieldValidationException.class);
			throw new RuntimeException("Failed to generate key", t);
		}
	}

	protected final static int symmetricKeyAlgorithmNameToTag(String algorithmName) {
		return getStaticFieldValue(algorithmName, SymmetricKeyAlgorithmTags.class);
	}

	protected final static int hashAlgorithmNameToTag(String algorithmName) {
		return getStaticFieldValue(algorithmName, HashAlgorithmTags.class);
	}

	protected final static int algorithmNameToTag(String algorithmName) {
		return getStaticFieldValue(algorithmName, PublicKeyAlgorithmTags.class);
	}

	private static int getStaticFieldValue(String fieldName, Class<?> clazz) {
		try {
			return clazz.getField(fieldName).getInt(null);
		} catch (NoSuchFieldException e) {
			throw new IllegalArgumentException("No such field " + fieldName + " defined in class " + clazz);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert algorithm name " + fieldName + " to algorithm tag", e);
		}
	}

	@SuppressWarnings("deprecation")
	private Key buildKey(PGPKeyRingGenerator keyRingGen) throws PGPException {
		Key ret = new Key();
		KeyDataPgp keyData = new KeyDataPgp();
		keyData.setPublicKeyRing(keyRingGen.generatePublicKeyRing());
		keyData.setSecretKeyRing(keyRingGen.generateSecretKeyRing());
		ret.setKeyData(keyData);
		ret.setKeyInfo(KeyFilesOperationsPgpImpl.buildKeyInfoFromSecret(keyData.getSecretKeyRing()));
		return ret;
	}

	/**
	 * NOTE: It feels like a little over-engineered thing since generation takes
	 * like 1 second not that long as it was advertised. So perhaps we might decide
	 * to get rid of it and run it on demand
	 * 
	 * @param params
	 * @return
	 * @throws Exception
	 */
	private KeyPair getOrGenerateKeyPair(KeyPairParams params) throws Exception {
		Future<KeyPair> future = pregeneratedKeyPairs.remove(params);
		if (future == null) {
			return PrecalculateDsaKeyPair.generateKeyPair(params);
		}
		return future.get();
	}

	private void assertParamsValid(CreateKeyParams params) throws FieldValidationException {
		ValidationContext ctx = new ValidationContext();

		ctx.validateNotEmpty(params.getFullName(), CreateKeyParams.FN_FULL_NAME);
		if (ctx.validateNotEmpty(params.getEmail(), CreateKeyParams.FN_EMAIL)) {
			ctx.validateEmailFormat(params.getEmail(), CreateKeyParams.FN_EMAIL);
		}
		if (ctx.validateNotEmpty(params.getPassphrase(), CreateKeyParams.FN_PASSPHRASE)
				&& ctx.validateNotEmpty(params.getPassphraseAgain(), CreateKeyParams.FN_PASSPHRASE_AGAIN)) {
			ctx.equals(params.getPassphrase(), "term." + CreateKeyParams.FN_PASSPHRASE, params.getPassphraseAgain(),
					"term." + CreateKeyParams.FN_PASSPHRASE_AGAIN, CreateKeyParams.FN_PASSPHRASE_AGAIN);
		}

		if (ctx.getHasErrors()) {
			throw new FieldValidationException(ctx.getErrors());
		}
	}

	@Override
	public void expectNewKeyCreation() {
		KeyPairParams params = getMasterKeyParameters();
		if (pregeneratedKeyPairs.containsKey(params)) {
			// there is already a pre-generated key params
			return;
		}
		log.info("Pre-generating master key in a background to improve user experience");
		Future<KeyPair> future = executorService.submit(new PrecalculateDsaKeyPair(params));
		pregeneratedKeyPairs.put(params, future);
	}

	public String getEncryptionKeyAlgorithm() {
		return encryptionKeyAlgorithm;
	}

	@Required
	public void setEncryptionKeyAlgorithm(String encryptionKeyAlgorithm) {
		this.encryptionKeyAlgorithm = encryptionKeyAlgorithm;
	}

	public KeyPairParams getMasterKeyParameters() {
		if (masterKeyParameters == null) {
			masterKeyParameters = new KeyPairParams(masterKeyAlgorithm, PROVIDER, masterKeySize);
		}
		return masterKeyParameters;
	}

	public String getMasterKeyAlgorithm() {
		return masterKeyAlgorithm;
	}

	@Required
	public void setMasterKeyAlgorithm(String masterKeyAlgorithm) {
		this.masterKeyAlgorithm = masterKeyAlgorithm;
		masterKeyParameters = null;
	}

	public int getMasterKeySize() {
		return masterKeySize;
	}

	@Required
	public void setMasterKeySize(int masterKeySize) {
		this.masterKeySize = masterKeySize;
		masterKeyParameters = null;
	}

	public BigInteger getDhParamsPrimeModulus() {
		return dhParamsPrimeModulus;
	}

	@Required
	public void setDhParamsPrimeModulus(BigInteger dhParamsPrimeModulus) {
		this.dhParamsPrimeModulus = dhParamsPrimeModulus;
	}

	public BigInteger getDhParamsBaseGenerator() {
		return dhParamsBaseGenerator;
	}

	@Required
	public void setDhParamsBaseGenerator(BigInteger dhParamsBaseGenerator) {
		this.dhParamsBaseGenerator = dhParamsBaseGenerator;
	}

	public String getEncryptionKeyPurpose() {
		return encryptionKeyPurpose;
	}

	@Required
	public void setEncryptionKeyPurpose(String encryptionKeyPurpose) {
		this.encryptionKeyPurpose = encryptionKeyPurpose;
	}

	public String getMasterKeySignerHashingAlgorithm() {
		return masterKeySignerHashingAlgorithm;
	}

	@Required
	public void setMasterKeySignerHashingAlgorithm(String masterKeySignerHashingAlgorithm) {
		this.masterKeySignerHashingAlgorithm = masterKeySignerHashingAlgorithm;
	}

	public String getSecretKeyHashingAlgorithm() {
		return secretKeyHashingAlgorithm;
	}

	@Required
	public void setSecretKeyHashingAlgorithm(String secretKeyHashingAlgorithm) {
		this.secretKeyHashingAlgorithm = secretKeyHashingAlgorithm;
	}

	public String getSecretKeyEncryptionAlgorithm() {
		return secretKeyEncryptionAlgorithm;
	}

	@Required
	public void setSecretKeyEncryptionAlgorithm(String secretKeyEncryptionAlgorithm) {
		this.secretKeyEncryptionAlgorithm = secretKeyEncryptionAlgorithm;
	}

	public String getMasterKeySignerAlgorithm() {
		return masterKeySignerAlgorithm;
	}

	@Required
	public void setMasterKeySignerAlgorithm(String masterKeySignerAlgorithm) {
		this.masterKeySignerAlgorithm = masterKeySignerAlgorithm;
	}

	public String getMasterKeyPurpose() {
		return masterKeyPurpose;
	}

	@Required
	public void setMasterKeyPurpose(String masterKeyPurpose) {
		this.masterKeyPurpose = masterKeyPurpose;
	}

	public static class PrecalculateDsaKeyPair implements Callable<KeyPair> {
		private KeyPairParams keyPairParams;

		public PrecalculateDsaKeyPair(KeyPairParams keyPairParams) {
			this.keyPairParams = keyPairParams;
		}

		@Override
		public KeyPair call() throws Exception {
			log.debug("Invoking pregeneration of a key pair " + keyPairParams);
			try {
				return generateKeyPair(keyPairParams);
			} finally {
				log.debug("Invocation completed " + keyPairParams);
			}
		}

		public static KeyPair generateKeyPair(KeyPairParams keyPairParams) throws Exception {
			try {
				log.debug("Calculating KeyPair " + keyPairParams);
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyPairParams.algorithm,
						keyPairParams.provider);
				keyPairGenerator.initialize(keyPairParams.keysize);
				log.info("Started key generation");
				KeyPair keyPair = keyPairGenerator.generateKeyPair();
				log.info("Key generation is complete");

				// byte[] encoded =
				// Base64.getEncoder().encode(keyPair.getPrivate().getEncoded());
				// String pkey = new String(encoded, "UTF-8");
				// log.debug("generated private key: " + pkey);

				return keyPair;
			} catch (Throwable t) {
				log.error("Failed to generate DSA keypair " + keyPairParams, t);
				throw new Exception("Failed to generate DSA keypair" + keyPairParams, t);
			}
		}
	}

	public static class KeyPairParams {
		String algorithm;
		String provider;
		int keysize;

		public KeyPairParams(String algorithm, String provider, int keysize) {
			this.algorithm = algorithm;
			this.provider = provider;
			this.keysize = keysize;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((algorithm == null) ? 0 : algorithm.hashCode());
			result = prime * result + keysize;
			result = prime * result + ((provider == null) ? 0 : provider.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			KeyPairParams other = (KeyPairParams) obj;
			if (algorithm == null) {
				if (other.algorithm != null) {
					return false;
				}
			} else if (!algorithm.equals(other.algorithm)) {
				return false;
			}
			if (keysize != other.keysize) {
				return false;
			}
			if (provider == null) {
				if (other.provider != null) {
					return false;
				}
			} else if (!provider.equals(other.provider)) {
				return false;
			}
			return true;
		}

		@Override
		public String toString() {
			return "KeyPairParams [algorithm=" + algorithm + ", PROVIDER=" + provider + ", keysize=" + keysize + "]";
		}
	}

}
