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
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.pgptool.gui.encryption.api.KeyGeneratorService;
import org.pgptool.gui.encryption.api.dto.ChangePasswordParams;
import org.pgptool.gui.encryption.api.dto.ChangeUserIdParams;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.summerb.utils.objectcopy.DeepCopy;
import org.summerb.validation.ValidationContext;
import org.summerb.validation.ValidationContextFactory;
import org.summerb.validation.ValidationException;

public class KeyGeneratorServicePgpImpl implements KeyGeneratorService {
  private static final Logger log = LoggerFactory.getLogger(KeyGeneratorServicePgpImpl.class);
  private static final String PROVIDER = "BC";

  private final ValidationContextFactory validationContextFactory;

  public KeyGeneratorServicePgpImpl(ValidationContextFactory validationContextFactory) {
    this.validationContextFactory = validationContextFactory;
    KeyRingServicePgpImpl.touch();
  }

  @Override
  public Key createNewKey(CreateKeyParams params, boolean emptyPassphraseConsent) {
    try {
      Preconditions.checkArgument(params != null, "params must not be null");
      assertParamsValid(params, emptyPassphraseConsent);

      // 1) Create JCA keypairs
      KeyPairGenerator kpgEd = KeyPairGenerator.getInstance("Ed25519", PROVIDER);
      KeyPairGenerator kpgX = KeyPairGenerator.getInstance("X25519", PROVIDER);

      log.debug("Generating master keypair");
      KeyPair kpPrimary = kpgEd.generateKeyPair(); // primary (certify)
      log.debug("Generating signing subkeypair");
      KeyPair kpSign = kpgEd.generateKeyPair(); // signing subkey
      log.debug("Generating encryption subkeypair");
      KeyPair kpEnc = kpgX.generateKeyPair(); // encryption subkey (ECDH)

      Date now = new Date();

      // 2) Wrap as PGPKeyPair with algorithm tags
      log.debug("Wrapping primary keypair as PGPKeyPair");
      // NOTE: I'm using deprecated PublicKeyAlgorithmTags.EDDSA instead of
      // PublicKeyAlgorithmTags.Ed25519 because i.e. GPG would not recognize algorithmId 27
      PGPKeyPair pkpPrimary =
          new JcaPGPKeyPair(
              PublicKeyPacket.VERSION_4, PublicKeyAlgorithmTags.EDDSA, kpPrimary, now);
      log.debug("Wrapping signing subkeypair as PGPKeyPair");
      PGPKeyPair pkpSign =
          new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PublicKeyAlgorithmTags.EDDSA, kpSign, now);
      log.debug("Wrapping encryption subkeypair as PGPKeyPair");
      PGPKeyPair pkpEnc =
          new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PublicKeyAlgorithmTags.ECDH, kpEnc, now);

      // 3) Digest calculators
      PGPDigestCalculator sha256 = buildDigestCalculatorForSecretKeyEncryption();

      // 4) Protect secret keys with AES-256 + strong S2K
      PBESecretKeyEncryptor secKeyEncryptor =
          buildKeyEncryptor(params.getPassphrase(), emptyPassphraseConsent, sha256);

      // 5) Content signer for self-sigs (use primary’s algorithm + hash)
      BcPGPContentSignerBuilder signerBuilder =
          new BcPGPContentSignerBuilder(PublicKeyAlgorithmTags.EDDSA, HashAlgorithmTags.SHA256);

      // 6) Primary UID self-signature subpackets (preferences)
      log.debug("Building primary self-signature subpackets");
      PGPSignatureSubpacketGenerator primaryHashed = new PGPSignatureSubpacketGenerator();
      primaryHashed.setKeyFlags(false, KeyFlags.CERTIFY_OTHER);
      setPreferences(primaryHashed);
      // Features (MDC; AEAD if you know peers support it)
      primaryHashed.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

      log.debug("Generating PGPSignatureSubpacketVector");
      PGPSignatureSubpacketVector primaryHashedV = primaryHashed.generate();

      // 7) Create key ring generator
      log.debug("Building PGPKeyRingGenerator");
      PGPKeyRingGenerator krg =
          new PGPKeyRingGenerator(
              PGPSignature.POSITIVE_CERTIFICATION,
              pkpPrimary,
              buildUserId(params.getFullName(), params.getEmail()),
              new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1),
              primaryHashedV,
              null, // unhashed subpackets
              signerBuilder,
              secKeyEncryptor);

      // 8) Add signing subkey with flags
      log.debug("Adding signing subkey");
      PGPSignatureSubpacketGenerator signHashed = new PGPSignatureSubpacketGenerator();
      signHashed.setKeyFlags(false, KeyFlags.SIGN_DATA);
      // Optional: set subkey expiration, e.g., 1 year (in seconds)
      // signHashed.setKeyExpirationTime(false, 365L * 24 * 60 * 60);
      krg.addSubKey(pkpSign, signHashed.generate(), null);

      // 9) Add encryption subkey with flags
      log.debug("Adding encryption subkey");
      PGPSignatureSubpacketGenerator encHashed = new PGPSignatureSubpacketGenerator();
      encHashed.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);
      // Optional expiration here too
      krg.addSubKey(pkpEnc, encHashed.generate(), null);

      // 10) Produce rings
      log.debug("Generating key ring");
      return buildKey(krg);
    } catch (Throwable t) {
      Throwables.throwIfInstanceOf(t, ValidationException.class);
      throw new RuntimeException("Failed to generate key", t);
    }
  }

  private static PBESecretKeyEncryptor buildKeyEncryptor(
      String passphrase, boolean emptyPassphraseConsent, PGPDigestCalculator sha256) {
    PBESecretKeyEncryptor secKeyEncryptor;
    if (StringUtils.hasText(passphrase)) {
      log.debug("Building secret key encryptor");
      int s2kCount =
          0xE0; // BouncyCastle encodes this as an exponent; higher is harder. Tune for ~100ms.
      BcPBESecretKeyEncryptorBuilder secKeyEncBuilder =
          new BcPBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha256, s2kCount);
      secKeyEncryptor = secKeyEncBuilder.build(passphrase.toCharArray());
    } else if (emptyPassphraseConsent) {
      // Build an explicit NULL-algorithm encryptor with empty passphrase to keep
      // secret key material format complete and compatible with GnuPG import.
      log.debug("Building NULL secret key encryptor for empty passphrase");
      BcPBESecretKeyEncryptorBuilder secKeyEncBuilder =
          new BcPBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.NULL, sha256);
      secKeyEncryptor = secKeyEncBuilder.build(new char[0]);
    } else {
      throw new IllegalStateException(
          "Illegal state. Either passphrase or emptyPassphraseConsent must be set. Both are null.");
    }
    return secKeyEncryptor;
  }

  private static PGPDigestCalculator buildDigestCalculatorForSecretKeyEncryption()
      throws PGPException {
    return new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);
  }

  protected static int algorithmNameToTag(String algorithmName) {
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

  public static String findStaticFieldNameByIntValue(int value, Class<?> clazz) {
    return Arrays.stream(clazz.getFields())
        .filter(x -> x.getType() == int.class)
        .filter(
            x -> {
              try {
                return x.getInt(null) == value;
              } catch (IllegalAccessException e) {
                return false;
              }
            })
        .map(Field::getName)
        .findFirst()
        .orElse(null);
  }

  @SuppressWarnings("deprecation")
  private Key buildKey(PGPKeyRingGenerator keyRingGen) throws PGPException {
    Key ret = new Key();
    KeyDataPgp keyData = new KeyDataPgp();
    keyData.setSecretKeyRing(keyRingGen.generateSecretKeyRing());
    keyData.setPublicKeyRing(keyRingGen.generatePublicKeyRing());
    ret.setKeyData(keyData);
    ret.setKeyInfo(KeyFilesOperationsPgpImpl.buildKeyInfoFromSecret(keyData.getSecretKeyRing()));
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
  public Key changeKeyPassword(Key key, ChangePasswordParams params, boolean emptyPasswordConsent) {
    assertParamsValid(params, emptyPasswordConsent);

    try {
      PGPDigestCalculator digestCalc = buildDigestCalculatorForSecretKeyEncryption();
      PBESecretKeyDecryptor decryptor =
          EncryptionServicePgpImpl.buildKeyDecryptor(params.getPassphrase());
      PBESecretKeyEncryptor encryptor =
          buildKeyEncryptor(params.getNewPassphrase(), emptyPasswordConsent, digestCalc);

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

  @Override
  public Key replacePrimaryUserId(Key key, ChangeUserIdParams params) {
    Preconditions.checkArgument(key != null, "key required");
    Preconditions.checkArgument(params != null, "params required");

    // 0) Validate user input first
    validateChangeUserIdParams(params);

    try {
      // 1) Derive new textual User ID from inputs
      String newUserId = buildUserId(params.getFullName(), params.getEmail());

      // 2) Work on a deep copy of the key (to keep original immutable)
      Key ret = DeepCopy.copyOrPopagateExcIfAny(key);
      KeyDataPgp keyData = (KeyDataPgp) ret.getKeyData();

      // 3) Find and unlock master secret key
      PGPSecretKeyRing secRing = keyData.getSecretKeyRing();
      Preconditions.checkArgument(secRing != null, "Secret key ring required to change User ID");
      PGPSecretKey masterSec = findMasterSecretKey(secRing);
      PGPPublicKey masterPub = masterSec.getPublicKey();
      PBESecretKeyDecryptor decryptor =
          EncryptionServicePgpImpl.buildKeyDecryptor(params.getPassphrase());
      PGPPrivateKey masterPriv = masterSec.extractPrivateKey(decryptor);

      // 4) Build a signer suitable for self-certifications
      BcPGPContentSignerBuilder signerBuilder = buildSignerFor(masterPub);

      // 5) Demote existing UIDs (primary=false), then add the new one as primary
      PGPPublicKey updatedMasterPub = demoteExistingUids(masterPub, signerBuilder, masterPriv);
      updatedMasterPub = addPrimaryUid(updatedMasterPub, newUserId, signerBuilder, masterPriv);

      // 6) Rebuild public ring with updated master first to satisfy BC invariants
      PGPPublicKeyRing currentPubRing = rebuildPublicRingFromSecretIfMissing(keyData);
      PGPPublicKeyRing newPubRing =
          rebuildPublicKeyRingWithUpdatedMaster(currentPubRing, masterPub, updatedMasterPub);
      keyData.setPublicKeyRing(newPubRing);

      // 7) Mirror updated public section into the secret ring
      PGPSecretKeyRing newSecRing =
          mirrorUpdatedMasterToSecretRing(secRing, masterSec, updatedMasterPub);
      keyData.setSecretKeyRing(newSecRing);

      // 8) Update KeyInfo for UI and return
      ret.getKeyInfo().setUser(newUserId);
      return ret;
    } catch (Throwable t) {
      throw new RuntimeException("Failed to change key User ID", t);
    }
  }

  private static PGPSecretKey findMasterSecretKey(PGPSecretKeyRing secRing) {
    PGPSecretKey masterSec = secRing.getSecretKey();
    if (masterSec != null && masterSec.isMasterKey()) {
      return masterSec;
    }
    for (PGPSecretKey sk : secRing) {
      if (sk.isMasterKey()) {
        return sk;
      }
    }
    throw new IllegalStateException("Master secret key not found");
  }

  private static BcPGPContentSignerBuilder buildSignerFor(PGPPublicKey masterPub) {
    return new BcPGPContentSignerBuilder(masterPub.getAlgorithm(), HashAlgorithmTags.SHA256);
  }

  private static PGPPublicKey demoteExistingUids(
      PGPPublicKey baseMasterPub, BcPGPContentSignerBuilder signerBuilder, PGPPrivateKey masterPriv)
      throws PGPException {
    PGPPublicKey updated = baseMasterPub;
    for (Iterator<String> it = baseMasterPub.getUserIDs(); it.hasNext(); ) {
      String uid = it.next();
      if (uid == null) {
        continue;
      }
      //noinspection deprecation -- fine for v4
      PGPSignatureGenerator sigGen = new PGPSignatureGenerator(signerBuilder);
      sigGen.init(PGPSignature.POSITIVE_CERTIFICATION, masterPriv);
      PGPSignatureSubpacketGenerator spg = new PGPSignatureSubpacketGenerator();
      spg.setPrimaryUserID(true, false); // explicitly not primary
      sigGen.setHashedSubpackets(spg.generate());
      PGPSignature sig = sigGen.generateCertification(uid, updated);
      updated = PGPPublicKey.addCertification(updated, uid, sig);
    }
    return updated;
  }

  private PGPPublicKey addPrimaryUid(
      PGPPublicKey updatedMasterPub,
      String newUserId,
      BcPGPContentSignerBuilder signerBuilder,
      PGPPrivateKey masterPriv)
      throws PGPException {
    //noinspection deprecation -- fine for v4
    PGPSignatureGenerator sigGen = new PGPSignatureGenerator(signerBuilder);
    sigGen.init(PGPSignature.POSITIVE_CERTIFICATION, masterPriv);
    PGPSignatureSubpacketGenerator spg = new PGPSignatureSubpacketGenerator();
    spg.setPrimaryUserID(true, true);
    setPreferences(spg); // include sane preferences on the new primary UID
    sigGen.setHashedSubpackets(spg.generate());
    PGPSignature cert = sigGen.generateCertification(newUserId, updatedMasterPub);
    return PGPPublicKey.addCertification(updatedMasterPub, newUserId, cert);
  }

  private static PGPPublicKeyRing rebuildPublicRingFromSecretIfMissing(KeyDataPgp keyData) {
    PGPPublicKeyRing pubRing = keyData.getPublicKeyRing();
    if (pubRing != null) {
      return pubRing;
    }
    PGPSecretKeyRing secRing = keyData.getSecretKeyRing();
    Preconditions.checkArgument(secRing != null, "No public ring; secret ring is required");
    List<PGPPublicKey> pubs = new ArrayList<>();
    for (Iterator<PGPPublicKey> it = secRing.getPublicKeys(); it.hasNext(); ) {
      pubs.add(it.next());
    }
    return new PGPPublicKeyRing(pubs);
  }

  private static PGPPublicKeyRing rebuildPublicKeyRingWithUpdatedMaster(
      PGPPublicKeyRing currentPubRing, PGPPublicKey oldMaster, PGPPublicKey updatedMaster) {
    // Keep master first; then add all other public keys in the original order
    List<PGPPublicKey> rebuilt = new ArrayList<>();
    rebuilt.add(updatedMaster);
    for (Iterator<PGPPublicKey> it = currentPubRing.getPublicKeys(); it.hasNext(); ) {
      PGPPublicKey pk = it.next();
      if (pk.getKeyID() == oldMaster.getKeyID()) {
        continue; // skip old master instance
      }
      rebuilt.add(pk);
    }
    return new PGPPublicKeyRing(rebuilt);
  }

  private static PGPSecretKeyRing mirrorUpdatedMasterToSecretRing(
      PGPSecretKeyRing secRing, PGPSecretKey oldMasterSec, PGPPublicKey updatedMasterPub) {
    PGPSecretKey newMasterSec = PGPSecretKey.replacePublicKey(oldMasterSec, updatedMasterPub);
    List<PGPSecretKey> secRebuilt = new ArrayList<>();
    secRebuilt.add(newMasterSec);
    for (Iterator<PGPSecretKey> it = secRing.getSecretKeys(); it.hasNext(); ) {
      PGPSecretKey sk = it.next();
      if (sk.getKeyID() == oldMasterSec.getKeyID()) {
        continue; // skip old master instance
      }
      secRebuilt.add(sk);
    }
    return new PGPSecretKeyRing(secRebuilt);
  }

  private static void setPreferences(PGPSignatureSubpacketGenerator target) {
    target.setPreferredSymmetricAlgorithms(
        false,
        new int[] {
          SymmetricKeyAlgorithmTags.AES_256,
          SymmetricKeyAlgorithmTags.AES_192,
          SymmetricKeyAlgorithmTags.AES_128
        });
    target.setPreferredHashAlgorithms(
        false,
        new int[] {
          HashAlgorithmTags.SHA256,
          HashAlgorithmTags.SHA512,
          HashAlgorithmTags.SHA384,
          HashAlgorithmTags.SHA224
        });
    target.setPreferredCompressionAlgorithms(
        false,
        new int[] {
          CompressionAlgorithmTags.ZLIB,
          CompressionAlgorithmTags.BZIP2,
          CompressionAlgorithmTags.ZIP
        });
  }

  private void validateChangeUserIdParams(ChangeUserIdParams params) {
    ValidationContext<ChangeUserIdParams> ctx = validationContextFactory.buildFor(params);
    ctx.hasText(ChangeUserIdParams::getFullName);
    if (ctx.hasText(ChangeUserIdParams::getEmail)) {
      ctx.validEmail(ChangeUserIdParams::getEmail);
    }
    ctx.throwIfHasErrors();
  }

  private static String buildUserId(String fullName, String email) {
    if (StringUtils.hasText(email)) {
      return fullName + " <" + email + ">";
    } else {
      return fullName;
    }
  }

  private void assertParamsValid(ChangePasswordParams params, boolean emptyPassphraseConsent) {
    ValidationContext<ChangePasswordParams> ctx = validationContextFactory.buildFor(params);

    if (!emptyPassphraseConsent) {
      ctx.hasText(ChangePasswordParams::getNewPassphrase);
    }
    if (StringUtils.hasText(params.getNewPassphrase())
        && ctx.hasText(ChangePasswordParams::getNewPassphraseAgain)) {
      ctx.eq(ChangePasswordParams::getNewPassphraseAgain, params.getNewPassphrase());
    }

    ctx.throwIfHasErrors();
  }
}
