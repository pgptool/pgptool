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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.ECDHPublicBCPGKey;
import org.bouncycastle.bcpg.ECDSAPublicBCPGKey;
import org.bouncycastle.bcpg.EdDSAPublicBCPGKey;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyInfo;
import org.pgptool.gui.encryption.api.dto.KeyTypeEnum;
import org.pgptool.gui.tools.IoStreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.summerb.methodCapturers.PropertyNameResolver;
import org.summerb.methodCapturers.PropertyNameResolverFactory;
import org.summerb.users.api.exceptions.InvalidPasswordException;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationException;

public class KeyFilesOperationsPgpImpl implements KeyFilesOperations {
  private static final Logger log = LoggerFactory.getLogger(KeyFilesOperationsPgpImpl.class);

  /**
   * Considering this as not a violation to DI since I don't see scenarios when we'll need to change
   * this
   */
  protected static final BcKeyFingerprintCalculator fingerprintCalculator =
      new BcKeyFingerprintCalculator();

  private final String passphraseFieldName;

  public KeyFilesOperationsPgpImpl(PropertyNameResolverFactory propertyNameResolverFactory) {
    PropertyNameResolver<CreateKeyParams> nameResolver =
        propertyNameResolverFactory.getResolver(CreateKeyParams.class);
    passphraseFieldName = nameResolver.resolve(CreateKeyParams::getPassphrase);
  }

  @Override
  public List<Key> readKeysFromFile(File file) {
    try (FileInputStream fis = new FileInputStream(file)) {
      List<Key> ret = new ArrayList<>();
      if (file.getName().endsWith(".asc")) {
        ArmoredInputSubStream subStream = new ArmoredInputSubStream(fis);
        while (subStream.hasNextSubStream()) {
          // A single armored block may contain a ring or a collection; parse all within it
          ret.addAll(readAllKeysFromStream(subStream));
        }
        Preconditions.checkArgument(!ret.isEmpty(), "No keys found");
        ret = combinePrivateAndPublicIfAny(ret);
      } else {
        // Parse entire stream and return all key rings found (public and/or secret),
        // then combine matching public/secret rings by key id.
        ret = readAllKeysFromStream(fis);
        Preconditions.checkArgument(!ret.isEmpty(), "No keys found");
        ret = combinePrivateAndPublicIfAny(ret);
      }
      return ret;
    } catch (Throwable t) {
      throw new RuntimeException("Can't read key file", t);
    }
  }

  /**
   * Read all key rings contained in the provided stream (binary or armored-decoded) and return a
   * list of Key objects, one per ring encountered. Unknown sections are skipped.
   */
  private List<Key> readAllKeysFromStream(InputStream stream) {
    List<Key> ret = new ArrayList<>();
    try {
      PGPObjectFactory factory =
          new PGPObjectFactory(PGPUtil.getDecoderStream(stream), fingerprintCalculator);
      for (Object section : factory) {
        if (section instanceof PGPSecretKeyRing) {
          ret.add(readPGPSecretKeyRing((PGPSecretKeyRing) section));
        } else if (section instanceof PGPSecretKeyRingCollection coll) {
          for (Iterator<PGPSecretKeyRing> it = coll.getKeyRings(); it.hasNext(); ) {
            ret.add(readPGPSecretKeyRing(it.next()));
          }
        } else if (section instanceof PGPPublicKeyRing) {
          ret.add(readPGPPublicKeyRing((PGPPublicKeyRing) section));
        } else if (section instanceof PGPPublicKeyRingCollection coll) {
          for (Iterator<PGPPublicKeyRing> it = coll.getKeyRings(); it.hasNext(); ) {
            ret.add(readPGPPublicKeyRing(it.next()));
          }
        } else {
          // Ignore other packet types during import
          log.debug("Skipping non-key section during import: " + section);
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException("Error happened while parsing keys", t);
    }
    return ret;
  }

  private Key readPGPPublicKeyRing(PGPPublicKeyRing section) throws PGPException {
    KeyDataPgp data = new KeyDataPgp();
    data.setPublicKeyRing(section);
    Key key = new Key();
    key.setKeyData(data);
    //noinspection deprecation
    key.setKeyInfo(buildKeyInfoFromPublic(section));
    return key;
  }

  private static Key readPGPSecretKeyRing(PGPSecretKeyRing section) throws PGPException {
    KeyDataPgp data = new KeyDataPgp();
    data.setSecretKeyRing(section);
    Key key = new Key();
    key.setKeyData(data);
    //noinspection deprecation
    key.setKeyInfo(buildKeyInfoFromSecret(section));
    return key;
  }

  private List<Key> combinePrivateAndPublicIfAny(List<Key> keys) {
    if (keys.size() == 1) {
      return keys;
    }

    List<Key> ret = new ArrayList<>();
    for (Key key : keys) {
      String keyId = key.getKeyInfo().getKeyId();
      Key existingKey =
          ret.stream()
              .filter(
                  x ->
                      keyId.equals(x.getKeyInfo().getKeyId())
                          || x.getKeyData().isHasAlternativeId(keyId))
              .findFirst()
              .orElse(null);

      if (existingKey == null) {
        ret.add(key);
      } else {
        if (!existingKey.getKeyData().isCanBeUsedForDecryption()
            && key.getKeyData().isCanBeUsedForDecryption()) {
          ret.remove(existingKey);
          ret.add(key);
        } else {
          // looks like a duplicate, ignore it
        }
      }
    }

    return ret;
  }

  @Override
  public List<Key> readKeysFromText(String text) throws GenericException {
    try {
      ArmoredInputSubStream subStream =
          new ArmoredInputSubStream(new ByteArrayInputStream(text.getBytes()));
      List<Key> ret = new ArrayList<>();
      while (subStream.hasNextSubStream()) {
        try {
          // Parse all keys within each armored block
          ret.addAll(readAllKeysFromStream(subStream));
        } catch (Throwable t) {
          throw new GenericException("warning.keyIsDamaged", t);
        }
      }
      if (ret.isEmpty()) {
        throw new GenericException("warning.noKeysFound");
      }
      ret = combinePrivateAndPublicIfAny(ret);
      return ret;
    } catch (Throwable t) {
      throw new GenericException("warning.couldNotReadAKey", t);
    }
  }

  private KeyInfo buildKeyInfoFromPublic(PGPPublicKeyRing publicKeyRing) throws PGPException {
    KeyInfo ret = new KeyInfo();
    ret.setKeyType(KeyTypeEnum.Public);
    PGPPublicKey key = publicKeyRing.getPublicKey();
    return fillKeyInfoFromPublicKey(ret, key);
  }

  private static KeyInfo fillKeyInfoFromPublicKey(KeyInfo ret, PGPPublicKey key)
      throws PGPException {
    ret.setUser(buildUser(key.getUserIDs()));
    ret.setKeyId(KeyDataPgp.buildKeyIdStr(key.getKeyID()));
    fillDates(ret, key);
    ret.setKeyAlgorithm(getAlgorithmName(key));
    return ret;
  }

  private static void fillDates(KeyInfo ret, PGPPublicKey key) {
    ret.setCreatedOn(new Date(key.getCreationTime().getTime()));
    if (key.getValidSeconds() != 0) {
      java.util.Date expiresAt =
          DateUtils.addSeconds(key.getCreationTime(), (int) key.getValidSeconds());
      ret.setExpiresAt(new Date(expiresAt.getTime()));
    }
  }

  private static String getAlgorithmName(PGPPublicKey key) {
    // Build a descriptive, reliable algorithm name based on the public key packet itself
    try {
      int algo = key.getAlgorithm();
      String base =
          switch (algo) {
            case PublicKeyAlgorithmTags.RSA_GENERAL,
                    PublicKeyAlgorithmTags.RSA_ENCRYPT,
                    PublicKeyAlgorithmTags.RSA_SIGN ->
                "RSA";
            case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT, PublicKeyAlgorithmTags.ELGAMAL_GENERAL ->
                "ELGAMAL";
            case PublicKeyAlgorithmTags.ECDSA -> "ECDSA " + resolveCurveName(key);
            case PublicKeyAlgorithmTags.ECDH ->
                // Might be classic NIST curve or modern X25519/X448 represented via ECDH
                resolveEcdhName(key);
            default -> {
              String algoName =
                  KeyGeneratorServicePgpImpl.findStaticFieldNameByIntValue(
                      algo, PublicKeyAlgorithmTags.class);
              yield algoName == null ? "algorithm-" + algo : algoName;
            }
          };

      int bits = key.getBitStrength();
      if (bits > 0) {
        return base + " " + bits + "bit";
      }
      return base;
    } catch (Throwable t) {
      log.warn(
          "Failed to build algorithm name for key {} algo {}",
          key.getKeyID(),
          key.getAlgorithm(),
          t);
      // As a last resort, keep previous fallback behavior to avoid UI breakage
      try {
        PGPSignature sig = key.getSignatures().next();
        return PGPUtil.getSignatureName(sig.getKeyAlgorithm(), sig.getHashAlgorithm());
      } catch (Exception e) {
        log.warn("... PGPUtil.getSignatureName failed as well", t);
        return "unresolved";
      }
    }
  }

  private static String resolveEcdhName(PGPPublicKey key) {
    String curve = resolveCurveName(key);
    // Recognize RFC 8410 curves commonly used for ECDH
    if ("1.3.101.110".equals(curve)) {
      return "X25519";
    }
    if ("1.3.101.111".equals(curve)) {
      return "X448";
    }
    return "ECDH " + mapCurveOidToName(curve);
  }

  private static String resolveCurveName(PGPPublicKey key) {
    try {
      PublicKeyPacket pk = key.getPublicKeyPacket();
      int algo = key.getAlgorithm();
      if (algo == PublicKeyAlgorithmTags.ECDH) {
        ECDHPublicBCPGKey ecdh = (ECDHPublicBCPGKey) pk.getKey();
        return ecdh.getCurveOID().getId();
      } else if (algo == PublicKeyAlgorithmTags.ECDSA) {
        ECDSAPublicBCPGKey ecdsa = (ECDSAPublicBCPGKey) pk.getKey();
        return ecdsa.getCurveOID().getId();
      } else if (algo == PublicKeyAlgorithmTags.Ed25519 || algo == PublicKeyAlgorithmTags.Ed448) {
        // EdDSA keys are on fixed curves, but some BC versions expose an EdDSA key object
        // If curve OID is available, return it; otherwise return known RFC 8410 OIDs
        try {
          EdDSAPublicBCPGKey ed = (EdDSAPublicBCPGKey) pk.getKey();
          if (ed.getCurveOID() != null) {
            return ed.getCurveOID().getId();
          }
        } catch (Throwable ex) {
          log.warn("Failed to resolve curve name for key " + key.getKeyID(), ex);
          // fall-through to constants
        }
        return algo == PublicKeyAlgorithmTags.Ed25519 ? "1.3.101.112" : "1.3.101.113";
      }
    } catch (Throwable ex) {
      log.warn("Failed to resolve curve name for key " + key.getKeyID(), ex);
      // ignore and fall back
    }
    return "unresolved";
  }

  private static String mapCurveOidToName(String oid) {
    if (oid == null || oid.isEmpty()) {
      return "unknown-curve";
    }
    return switch (oid) {
      case "1.3.101.112" -> "Ed25519";
      case "1.3.101.113" -> "Ed448";
      case "1.3.101.110" -> "X25519";
      case "1.3.101.111" -> "X448";
      case "1.2.840.10045.3.1.7" -> "P-256";
      case "1.3.132.0.34" -> "P-384";
      case "1.3.132.0.35" -> "P-521";
      case "1.3.132.0.10" -> "secp256k1";
      default -> oid; // show raw OID if unknown
    };
  }

  protected static KeyInfo buildKeyInfoFromSecret(PGPSecretKeyRing secretKeyRing)
      throws PGPException {
    KeyInfo ret = new KeyInfo();
    ret.setKeyType(KeyTypeEnum.KeyPair);
    PGPPublicKey key = secretKeyRing.getPublicKey();
    return fillKeyInfoFromPublicKey(ret, key);
  }

  private static String buildUser(Iterator<String> userIDs) {
    if (userIDs == null) {
      return "(no user id)";
    }
    try {
      if (userIDs.hasNext()) {
        return userIDs.next();
      }
    } catch (Throwable exc) {
      log.warn("Failed to read user id from secret key", exc);
    }
    return "(no user id)";
  }

  @Override
  public void exportPublicKey(Key key, String targetFilePathname) {
    Preconditions.checkArgument(
        StringUtils.hasText(targetFilePathname), "targetFilePathname must be provided");
    try {
      FileOutputStream fos = new FileOutputStream(targetFilePathname);
      boolean saveAsArmored =
          "asc".equalsIgnoreCase(FilenameUtils.getExtension(targetFilePathname));
      savePublicKey(key, fos, saveAsArmored);
    } catch (Throwable t) {
      throw new RuntimeException(
          "Failed to export public key " + key.getKeyInfo().getUser() + " to " + targetFilePathname,
          t);
    }
  }

  private void savePublicKey(Key key, OutputStream outputStream, boolean saveAsArmored) {
    Preconditions.checkArgument(
        key != null && key.getKeyData() != null && key.getKeyInfo() != null,
        "Key must be providedand fully described");
    Stack<OutputStream> os = new Stack<>();
    try {
      os.push(outputStream);
      if (saveAsArmored) {
        os.push(new ArmoredOutputStream(os.peek()));
      }
      KeyDataPgp keyDataPgp = KeyDataPgp.get(key);
      PGPPublicKeyRing pubRing = keyDataPgp.getPublicKeyRing();
      if (pubRing == null) {
        // Rebuild a complete public ring from the secret ring's public keys (primary + subkeys)
        Preconditions.checkArgument(
            keyDataPgp.getSecretKeyRing() != null, "No public or secret key ring available");
        List<PGPPublicKey> allPubs = new ArrayList<>();
        Iterator<PGPPublicKey> it = keyDataPgp.getSecretKeyRing().getPublicKeys();
        while (it.hasNext()) {
          allPubs.add(it.next());
        }
        pubRing = new PGPPublicKeyRing(allPubs);
      }
      pubRing.encode(os.peek());
    } catch (Throwable t) {
      throw new RuntimeException("Failed to save public key " + key.getKeyInfo().getUser(), t);
    } finally {
      while (!os.isEmpty()) {
        IoStreamUtils.safeClose(os.pop());
      }
    }
  }

  @Override
  public String getPublicKeyArmoredRepresentation(Key key) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    savePublicKey(key, baos, true);
    return baos.toString(StandardCharsets.UTF_8);
  }

  @Override
  public void exportPrivateKey(Key key, String targetFilePathname) {
    Preconditions.checkArgument(
        key != null && key.getKeyData() != null && key.getKeyInfo() != null,
        "Key must be provided and fully described");
    KeyDataPgp keyDataPgp = KeyDataPgp.get(key);
    Preconditions.checkArgument(
        keyDataPgp.getSecretKeyRing() != null, "KeyPair key wasn't provided");
    Preconditions.checkArgument(
        StringUtils.hasText(targetFilePathname), "targetFilePathname must be provided");
    Stack<OutputStream> os = new Stack<>();
    try {
      os.push(new FileOutputStream(targetFilePathname));
      if ("asc".equalsIgnoreCase(FilenameUtils.getExtension(targetFilePathname))) {
        os.push(new ArmoredOutputStream(os.peek()));
      }
      keyDataPgp.getSecretKeyRing().encode(os.peek());
      // Do NOT also encode the public key ring here; secret ring already contains necessary public
      // packets
    } catch (Throwable t) {
      throw new RuntimeException(
          "Failed to export private key "
              + key.getKeyInfo().getUser()
              + " to "
              + targetFilePathname,
          t);
    } finally {
      while (!os.isEmpty()) {
        IoStreamUtils.safeClose(os.pop());
      }
    }
  }

  @Override
  public void validateDecryptionKeyPassword(String secretKeyId, Key key, String password) {
    try {
      validatePasswordUnchecked(secretKeyId, key, password);
    } catch (InvalidPasswordException pe) {
      throw new ValidationException(new ValidationError(pe.getMessageCode(), passphraseFieldName));
    } catch (Throwable t) {
      throw new RuntimeException("Failed to verify key password", t);
    }
  }

  @Override
  public void validateKeyPassword(Key key, String passphrase) {
    try {
      KeyDataPgp keyData = (KeyDataPgp) key.getKeyData();
      for (PGPSecretKey secretKey : keyData.getSecretKeyRing()) {
        String keyIdStr = KeyDataPgp.buildKeyIdStr(secretKey.getKeyID());
        KeyFilesOperationsPgpImpl.validatePasswordUnchecked(keyIdStr, key, passphrase);
      }
    } catch (InvalidPasswordException pe) {
      throw new ValidationException(new ValidationError(pe.getMessageCode(), passphraseFieldName));
    } catch (Throwable t) {
      throw new RuntimeException(
          "Unknown failure during attempt to verify current key password", t);
    }
  }

  protected static void validatePasswordUnchecked(String secretKeyId, Key key, String password)
      throws InvalidPasswordException {
    PGPSecretKey secretKey = KeyDataPgp.get(key).findSecretKeyById(secretKeyId);
    Preconditions.checkArgument(secretKey != null, "Matching secret key wasn't found");

    // NOTE: When actual key does not have any password then getPrivateKey() will
    // succeed with any password provided. Which is weird. So I'm enforcing check on
    // empty password myself
    if (isActuallyHasEmptyPassword(secretKey) && StringUtils.hasText(password)) {
      throw new InvalidPasswordException();
    }

    // Now regular password check
    PGPPrivateKey privateKey = getPrivateKey(password, secretKey);
    Preconditions.checkArgument(privateKey != null, "Failed to extract private key");
  }

  protected static boolean isActuallyHasEmptyPassword(PGPSecretKey secretKey) {
    try {
      PGPPrivateKey privateKey = getPrivateKey(null, secretKey);
      Preconditions.checkArgument(privateKey != null, "Failed to extract private key");
      return true;
    } catch (InvalidPasswordException e) {
      // that's ok
      return false;
    }
  }

  protected static PGPPrivateKey getPrivateKey(String passphrase, PGPSecretKey secretKey)
      throws InvalidPasswordException {
    try {
      PBESecretKeyDecryptor decryptorFactory =
          EncryptionServicePgpImpl.buildKeyDecryptor(passphrase);
      return secretKey.extractPrivateKey(decryptorFactory);
    } catch (PGPException pgpe) {
      if (pgpe.getMessage() == null || !pgpe.getMessage().contains("checksum mismatch")) {
        log.debug("Can't extract private key. Most likely passphrase is incorrect", pgpe);
      }
      throw new InvalidPasswordException();
    } catch (Throwable t) {
      throw new RuntimeException("Failed to extract private key", t);
    }
  }

  @Override
  public Key readKeyFromFile(String fileName) {
    try {
      List<Key> ret = readKeysFromFile(new File(fileName));
      Preconditions.checkArgument(ret.size() == 1, "Exactly one key is expected");
      return ret.get(0);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to read key", t);
    }
  }
}
