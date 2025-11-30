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

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;
import java.util.Map;
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
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.dto.ChangePasswordParams;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.springframework.util.StringUtils;
import org.summerb.utils.objectcopy.DeepCopy;
import org.summerb.validation.ValidationContext;
import org.summerb.validation.ValidationContextFactory;
import org.summerb.validation.ValidationException;

public class KeyGeneratorServicePgpImpl implements KeyGeneratorService {
  private static final Logger log = Logger.getLogger(KeyGeneratorServicePgpImpl.class);
  private static final String PROVIDER = "BC";

  private final ExecutorService executorService;
  private final ValidationContextFactory validationContextFactory;

  // Master key params
  private final String masterKeyAlgorithm;
  private final String masterKeyPurpose;
  private final int masterKeySize;
  private final String masterKeySignerAlgorithm;
  private final String masterKeySignerHashingAlgorithm;

  // Secret Key encryption
  private final String secretKeyHashingAlgorithm;
  private final String secretKeyEncryptionAlgorithm;

  // Encryption key params
  private final String encryptionKeyAlgorithm;
  private final String encryptionKeyPurpose;
  private final BigInteger dhParamsPrimeModulus;
  private final BigInteger dhParamsBaseGenerator;

  private KeyPairParams masterKeyParameters;
  private final Map<KeyPairParams, Future<KeyPair>> pregeneratedKeyPairs =
      new ConcurrentHashMap<>();

  public KeyGeneratorServicePgpImpl(
      ExecutorService executorService,
      ValidationContextFactory validationContextFactory,
      String masterKeyAlgorithm,
      String masterKeyPurpose,
      int masterKeySize,
      String masterKeySignerAlgorithm,
      String masterKeySignerHashingAlgorithm,
      String secretKeyHashingAlgorithm,
      String secretKeyEncryptionAlgorithm,
      String encryptionKeyAlgorithm,
      String encryptionKeyPurpose,
      BigInteger dhParamsPrimeModulus,
      BigInteger dhParamsBaseGenerator) {
    this.executorService = executorService;
    this.validationContextFactory = validationContextFactory;
    this.masterKeyAlgorithm = masterKeyAlgorithm;
    this.masterKeyPurpose = masterKeyPurpose;
    this.masterKeySize = masterKeySize;
    this.masterKeySignerAlgorithm = masterKeySignerAlgorithm;
    this.masterKeySignerHashingAlgorithm = masterKeySignerHashingAlgorithm;
    this.secretKeyHashingAlgorithm = secretKeyHashingAlgorithm;
    this.secretKeyEncryptionAlgorithm = secretKeyEncryptionAlgorithm;
    this.encryptionKeyAlgorithm = encryptionKeyAlgorithm;
    this.encryptionKeyPurpose = encryptionKeyPurpose;
    this.dhParamsPrimeModulus = dhParamsPrimeModulus;
    this.dhParamsBaseGenerator = dhParamsBaseGenerator;
    KeyRingServicePgpImpl.touch();
  }

  @Override
  public Key createNewKey(CreateKeyParams params, boolean emptyPassphraseConsent) {
    try {
      Preconditions.checkArgument(params != null, "params must not be null");
      assertParamsValid(params, emptyPassphraseConsent);

      // Create Master key
      KeyPair masterKey = getOrGenerateKeyPair(getMasterKeyParameters());
      PGPKeyPair masterKeyBc =
          new JcaPGPKeyPair(algorithmNameToTag(masterKeyPurpose), masterKey, new Date());
      BcPGPContentSignerBuilder keySignerBuilderBc =
          new BcPGPContentSignerBuilder(
              algorithmNameToTag(masterKeyPurpose),
              hashAlgorithmNameToTag(masterKeySignerHashingAlgorithm));

      // Setup seret key encryption
      PGPDigestCalculator digestCalc = buildDigestCalc();
      PBESecretKeyEncryptor keyEncryptorBc =
          buildKeyEncryptor(digestCalc, params.getPassphrase(), emptyPassphraseConsent);

      // Key pair generator
      String userName = params.getFullName() + " <" + params.getEmail() + ">";
      PGPKeyRingGenerator keyPairGeneratorBc =
          new PGPKeyRingGenerator(
              PGPSignature.POSITIVE_CERTIFICATION,
              masterKeyBc,
              userName,
              digestCalc,
              null,
              null,
              keySignerBuilderBc,
              keyEncryptorBc);

      // Add Sub-key for encryption
      KeyPairGenerator keyPairGenerator =
          KeyPairGenerator.getInstance(encryptionKeyAlgorithm, PROVIDER);
      if ("ELGAMAL".equals(encryptionKeyAlgorithm)) {
        keyPairGenerator.initialize(
            new DHParameterSpec(dhParamsPrimeModulus, dhParamsBaseGenerator));
      } else if ("RSA".equals(encryptionKeyAlgorithm)) {
        // Re-using master key size.
        keyPairGenerator.initialize(
            new RSAKeyGenParameterSpec(masterKeySize, RSAKeyGenParameterSpec.F4));
      } else {
        throw new IllegalArgumentException(
            "Hanlding of parameter creation for " + encryptionKeyAlgorithm + " is not implemented");
      }
      KeyPair encryptionSubKey = keyPairGenerator.generateKeyPair();
      PGPKeyPair encryptionSubKeyBc =
          new JcaPGPKeyPair(algorithmNameToTag(encryptionKeyPurpose), encryptionSubKey, new Date());
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
      Throwables.throwIfInstanceOf(t, ValidationException.class);
      throw new RuntimeException("Failed to generate key", t);
    }
  }

  protected PGPDigestCalculator buildDigestCalc() throws PGPException {
    return new BcPGPDigestCalculatorProvider()
        .get(hashAlgorithmNameToTag(secretKeyHashingAlgorithm));
  }

  private char[] toCharArray(String optionalPasshprase) {
    if (optionalPasshprase == null) {
      return new char[0];
    }
    return optionalPasshprase.toCharArray();
  }

  private String getSecretKeyEncryptionAlgorithmForOptionalPassphrase(
      boolean emptyPassphraseConsent) {
    if (emptyPassphraseConsent) {
      return /* SymmetricKeyAlgorithmTags. */ "NULL";
    }
    return secretKeyEncryptionAlgorithm;
  }

  protected static final int symmetricKeyAlgorithmNameToTag(String algorithmName) {
    return getStaticFieldValue(algorithmName, SymmetricKeyAlgorithmTags.class);
  }

  protected static final int hashAlgorithmNameToTag(String algorithmName) {
    return getStaticFieldValue(algorithmName, HashAlgorithmTags.class);
  }

  protected static final int algorithmNameToTag(String algorithmName) {
    return getStaticFieldValue(algorithmName, PublicKeyAlgorithmTags.class);
  }

  private static int getStaticFieldValue(String fieldName, Class<?> clazz) {
    try {
      return clazz.getField(fieldName).getInt(null);
    } catch (NoSuchFieldException e) {
      throw new IllegalArgumentException(
          "No such field " + fieldName + " defined in class " + clazz);
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to convert algorithm name " + fieldName + " to algorithm tag", e);
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
   * NOTE: It feels like a little over-engineered thing since generation takes like 1 second not
   * that long as it was advertised. So perhaps we might decide to get rid of it and run it on
   * demand
   *
   * @param params
   * @return
   * @throws Exception
   */
  private KeyPair getOrGenerateKeyPair(KeyPairParams params) throws Exception {
    log.debug("Checking if we have pregenerated master KeyPair");
    Future<KeyPair> future = pregeneratedKeyPairs.remove(params);
    if (future == null) {
      log.debug(
          "Creating master KeyPair via directly calling ProactivelyGenerateMasterKeyPair.generateKeyPair");
      return ProactivelyGenerateMasterKeyPair.generateKeyPair(params);
    }
    log.debug("Obtaining master KeyPair via pregenerated future");
    KeyPair ret = future.get();
    log.debug("master KeyPair obtained");
    return ret;
  }

  private void assertParamsValid(CreateKeyParams params, boolean emptyPassphraseConsent) {
    ValidationContext<CreateKeyParams> ctx = validationContextFactory.buildFor(params);

    ctx.hasText(CreateKeyParams::getFullName);
    if (ctx.hasText(CreateKeyParams::getEmail)) {
      ctx.validEmail(CreateKeyParams::getEmail);
    }

    if (!emptyPassphraseConsent) {
      ctx.hasText(CreateKeyParams::getPassphrase);
    }
    if (StringUtils.hasText(params.getPassphrase())
        && ctx.hasText(CreateKeyParams::getPassphraseAgain)) {

      ctx.eq(CreateKeyParams::getPassphraseAgain, params.getPassphrase());
    }

    ctx.throwIfHasErrors();
  }

  @Override
  public void expectNewKeyCreation() {
    KeyPairParams params = getMasterKeyParameters();
    if (pregeneratedKeyPairs.containsKey(params)) {
      // there is already a pre-generated key params
      return;
    }
    log.info("Proactively generating master key in a background to improve user experience");
    Future<KeyPair> future = executorService.submit(new ProactivelyGenerateMasterKeyPair(params));
    pregeneratedKeyPairs.put(params, future);
  }

  @Override
  public Key changeKeyPassword(Key key, ChangePasswordParams params, boolean emptyPasswordConsent) {
    assertParamsValid(params, emptyPasswordConsent);

    try {
      PGPDigestCalculator digestCalc = buildDigestCalc();
      PBESecretKeyDecryptor decryptor =
          EncryptionServicePgpImpl.buildKeyDecryptor(params.getPassphrase());
      PBESecretKeyEncryptor encryptor =
          buildKeyEncryptor(digestCalc, params.getNewPassphrase(), emptyPasswordConsent);

      Key ret = DeepCopy.copyOrPopagateExcIfAny(key);
      KeyDataPgp keyData = (KeyDataPgp) ret.getKeyData();
      for (PGPSecretKey secretKey : keyData.getSecretKeyRing()) {
        PGPSecretKey secretKey2 = PGPSecretKey.copyWithNewPassword(secretKey, decryptor, encryptor);
        keyData.setSecretKeyRing(
            PGPSecretKeyRing.insertSecretKey(keyData.getSecretKeyRing(), secretKey2));
      }

      return ret;
    } catch (Throwable t) {
      throw new RuntimeException("Change password failed", t);
    }
  }

  protected PBESecretKeyEncryptor buildKeyEncryptor(
      PGPDigestCalculator digestCalc, String password, boolean emptyPasswordConsent) {
    BcPBESecretKeyEncryptorBuilder encryptorBuilderBC =
        new BcPBESecretKeyEncryptorBuilder(
            symmetricKeyAlgorithmNameToTag(
                getSecretKeyEncryptionAlgorithmForOptionalPassphrase(emptyPasswordConsent)),
            digestCalc);
    return encryptorBuilderBC.build(toCharArray(password));
  }

  private void assertParamsValid(ChangePasswordParams params, boolean emptyPassphraseConsent) {
    ValidationContext<ChangePasswordParams> ctx = validationContextFactory.buildFor(params);

    if (!emptyPassphraseConsent) {
      ctx.hasText(ChangePasswordParams::getNewPassphrase);
    }
    if (StringUtils.hasText(params.getNewPassphrase())
        && ctx.hasText(ChangePasswordParams::getNewPassphraseAgain)) {
      ctx.eq(ChangePasswordParams::getNewPassphraseAgain, params.getPassphrase());
    }

    ctx.throwIfHasErrors();
  }

  public KeyPairParams getMasterKeyParameters() {
    if (masterKeyParameters == null) {
      masterKeyParameters = new KeyPairParams(masterKeyAlgorithm, PROVIDER, masterKeySize);
    }
    return masterKeyParameters;
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
      return "KeyPairParams [algorithm="
          + algorithm
          + ", PROVIDER="
          + provider
          + ", keysize="
          + keysize
          + "]";
    }
  }
}
