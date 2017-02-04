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

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.crypto.spec.DHParameterSpec;

import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
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
import org.springframework.beans.factory.DisposableBean;
import org.summerb.approaches.validation.FieldValidationException;
import org.summerb.approaches.validation.ValidationContext;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

public class KeyGeneratorServicePgpImpl implements KeyGeneratorService<KeyDataPgp>, DisposableBean {
	private static Logger log = Logger.getLogger(KeyGeneratorServicePgpImpl.class);

	// NOTE: Shouldn't I generate it each time. Is it safe to have it hardcoded?
	BigInteger g = new BigInteger(
			"153d5d6172adb43045b68ae8e1de1070b6137005686d29d3d73a7749199681ee5b212c9b96bfdcfa5b20cd5e3fd2044895d609cf9b410b7a0f12ca1cb9a428cc",
			16);
	BigInteger p = new BigInteger(
			"9494fec095f3b85ee286542b3836fc81a5dd0a0349b4c239dd38744d488cf8e31db8bcb7d33b41abb9e5a33cca9144b1cef332c94bf0573bf047a3aca98cdf3b",
			16);
	private static DsaKeyPairParams DEFAULT_DSA_KEY_PARAMETERS = new DsaKeyPairParams("DSA", "BC", 1024);

	private Map<DsaKeyPairParams, Future<KeyPair>> pregeneratedDsaKeyPairs = new ConcurrentHashMap<>();
	private ExecutorService executorService;

	public KeyGeneratorServicePgpImpl() {
		KeyRingServicePgpImpl.touch();
	}

	@Override
	public void destroy() throws Exception {
		if (executorService != null) {
			executorService.shutdownNow();
		}
	}

	@Override
	public Key<KeyDataPgp> createNewKey(CreateKeyParams params) throws FieldValidationException {
		try {
			Preconditions.checkArgument(params != null, "params must not be null");
			assertParamsValid(params);

			// Create KeyPairs
			KeyPair dsaKp = getOrGenerateDsaKeyPair(DEFAULT_DSA_KEY_PARAMETERS);
			KeyPairGenerator elgKpg = KeyPairGenerator.getInstance("ELGAMAL", "BC");
			DHParameterSpec elParams = new DHParameterSpec(p, g);
			elgKpg.initialize(elParams);
			KeyPair elgKp = elgKpg.generateKeyPair();

			// Now let do some crazy stuff (I HAVE NO IDEA WHAT I AM DOING
			// HERE). BouncyCastle guys are not helping by changing API from
			// one version to another so often!!!!!!!
			PGPKeyPair dsaKeyPair = new JcaPGPKeyPair(PGPPublicKey.DSA, dsaKp, new Date());
			PGPKeyPair elgKeyPair = new JcaPGPKeyPair(PGPPublicKey.ELGAMAL_ENCRYPT, elgKp, new Date());

			// PGPContentSignerBuilde
			// JCA
			// JcaPGPContentSignerBuilder keySignerBuilder = new
			// JcaPGPContentSignerBuilder(
			// dsaKeyPair.getPublicKey().getAlgorithm(),
			// HashAlgorithmTags.SHA1);

			// BC
			BcPGPContentSignerBuilder keySignerBuilderBC = new BcPGPContentSignerBuilder(
					dsaKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1);

			// PGPDigestCalculator
			// JCA
			// PGPDigestCalculator sha1Calc = new
			// JcaPGPDigestCalculatorProviderBuilder().build()
			// .get(HashAlgorithmTags.SHA1);

			// BC
			PGPDigestCalculator sha1CalcBC = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);

			// keyEncryptor
			// BC
			BcPBESecretKeyEncryptorBuilder encryptorBuilderBC = new BcPBESecretKeyEncryptorBuilder(
					PGPEncryptedData.AES_256, sha1CalcBC);
			PBESecretKeyEncryptor keyEncryptorBC = encryptorBuilderBC.build(params.getPassphrase().toCharArray());

			// JCA
			// JcePBESecretKeyEncryptorBuilder encryptorBuilder = new
			// JcePBESecretKeyEncryptorBuilder(
			// PGPEncryptedData.AES_256, sha1Calc).setProvider("BC");
			// PBESecretKeyEncryptor keyEncryptor =
			// encryptorBuilder.build(params.getPassphrase().toCharArray());

			// keyRingGen
			String userName = params.getFullName() + " <" + params.getEmail() + ">";
			// JCA
			// PGPKeyRingGenerator keyRingGen = new
			// PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION,
			// dsaKeyPair,
			// userName, sha1Calc, null, null, keySignerBuilder,
			// keyEncryptor);

			// BC
			PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, dsaKeyPair,
					userName, sha1CalcBC, null, null, keySignerBuilderBC, keyEncryptorBC);

			keyRingGen.addSubKey(elgKeyPair);
			// building ret
			Key<KeyDataPgp> ret = buildKey(keyRingGen);
			return ret;
		} catch (Throwable t) {
			Throwables.propagateIfInstanceOf(t, FieldValidationException.class);
			throw new RuntimeException("Failed to generate key", t);
		}
	}

	@SuppressWarnings("deprecation")
	private Key<KeyDataPgp> buildKey(PGPKeyRingGenerator keyRingGen) throws PGPException {
		Key<KeyDataPgp> ret = new Key<>();
		KeyDataPgp keyData = new KeyDataPgp();
		keyData.setPublicKeyRing(keyRingGen.generatePublicKeyRing());
		keyData.setSecretKeyRing(keyRingGen.generateSecretKeyRing());
		ret.setKeyData(keyData);
		ret.setKeyInfo(KeyFilesOperationsPgpImpl.buildKeyInfoFromSecret(keyData.getSecretKeyRing()));
		return ret;
	}

	/**
	 * NOTE: It feels like a little over-engineered thing since generation takes
	 * like 1 second not that long as it was advertised. So perhaps we might
	 * decide to get rid of it and run it on demand
	 * 
	 * @param params
	 * @return
	 * @throws Exception
	 */
	private KeyPair getOrGenerateDsaKeyPair(DsaKeyPairParams params) throws Exception {
		Future<KeyPair> future = pregeneratedDsaKeyPairs.remove(params);
		if (future == null) {
			return PrecalculateDsaKeyPair.generateDsaKeyPair(params);
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
		precalculateKeyPair(DEFAULT_DSA_KEY_PARAMETERS);
	}

	private void precalculateKeyPair(DsaKeyPairParams params) {
		Future<KeyPair> future = getExecutorService().submit(new PrecalculateDsaKeyPair(params));
		pregeneratedDsaKeyPairs.put(params, future);
	}

	public synchronized ExecutorService getExecutorService() {
		if (executorService == null) {
			executorService = Executors.newSingleThreadExecutor();
		}
		return executorService;
	}

	public static class PrecalculateDsaKeyPair implements Callable<KeyPair> {
		private DsaKeyPairParams dsaKeyPairParams;

		public PrecalculateDsaKeyPair(DsaKeyPairParams dsaKeyPairParams) {
			this.dsaKeyPairParams = dsaKeyPairParams;
		}

		@Override
		public KeyPair call() throws Exception {
			log.debug("Invoking pregeneration of DSA key pair " + dsaKeyPairParams);
			try {
				return generateDsaKeyPair(dsaKeyPairParams);
			} finally {
				log.debug("Invokation completed " + dsaKeyPairParams);
			}
		}

		public static KeyPair generateDsaKeyPair(DsaKeyPairParams dsaKeyPairParams) throws Exception {
			try {
				log.debug("Calculating DSA KeyPair " + dsaKeyPairParams);
				KeyPairGenerator dsaKpg = KeyPairGenerator.getInstance(dsaKeyPairParams.algorithm,
						dsaKeyPairParams.provider);
				dsaKpg.initialize(dsaKeyPairParams.keysize);
				KeyPair dsaKp = dsaKpg.generateKeyPair();
				return dsaKp;
			} catch (Throwable t) {
				log.error("Failed to generate DSA keypair " + dsaKeyPairParams, t);
				throw new Exception("Failed to generate DSA keypair" + dsaKeyPairParams, t);
			}
		}
	}

	public static class DsaKeyPairParams {
		String algorithm;
		String provider;
		int keysize;

		public DsaKeyPairParams(String algorithm, String provider, int keysize) {
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
			DsaKeyPairParams other = (DsaKeyPairParams) obj;
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
			return "DsaKeyPairParams [algorithm=" + algorithm + ", provider=" + provider + ", keysize=" + keysize + "]";
		}
	}

}
