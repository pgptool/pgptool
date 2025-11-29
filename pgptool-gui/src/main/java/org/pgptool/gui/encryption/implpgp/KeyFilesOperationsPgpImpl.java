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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
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
import org.pgptool.gui.app.GenericException;
import org.pgptool.gui.encryption.api.KeyFilesOperations;
import org.pgptool.gui.encryption.api.dto.CreateKeyParams;
import org.pgptool.gui.encryption.api.dto.Key;
import org.pgptool.gui.encryption.api.dto.KeyInfo;
import org.pgptool.gui.encryption.api.dto.KeyTypeEnum;
import org.pgptool.gui.tools.IoStreamUtils;
import org.springframework.util.StringUtils;
import org.summerb.users.api.exceptions.InvalidPasswordException;
import org.summerb.validation.ValidationError;
import org.summerb.validation.ValidationException;

public class KeyFilesOperationsPgpImpl implements KeyFilesOperations {
  private static final Logger log = Logger.getLogger(KeyFilesOperationsPgpImpl.class);

  /**
   * Considering this as not a violation to DI since I don't see scenarios when we'll need to change
   * this
   */
  protected static final BcKeyFingerprintCalculator fingerprintCalculator =
      new BcKeyFingerprintCalculator();

  @Override
  public List<Key> readKeysFromFile(File file) {
    try (FileInputStream fis = new FileInputStream(file)) {
      List<Key> ret = new ArrayList<>();
      if (file.getName().endsWith(".asc")) {
        ArmoredInputSubStream subStream = new ArmoredInputSubStream(fis);
        while (subStream.hasNextSubStream()) {
          ret.add(readFromStream(subStream));
        }
        Preconditions.checkArgument(!ret.isEmpty(), "No keys found");
        ret = combinePrivateAndPublicIfAny(ret);
      } else {
        ret.add(readFromStream(fis));
      }
      return ret;
    } catch (Throwable t) {
      throw new RuntimeException("Can't read key file", t);
    }
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
          // looks like a duplciate, ignore it
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
          ret.add(readFromStream(subStream));
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

  @SuppressWarnings("deprecation")
  private Key readFromStream(InputStream stream) throws PGPException {
    KeyDataPgp data = new KeyDataPgp();
    try {
      readKeyFromStream(data, stream);
    } catch (Throwable t) {
      throw new RuntimeException("Error happened while parsing key", t);
    }
    if (data.getPublicKeyRing() == null && data.getSecretKeyRing() == null) {
      throw new RuntimeException("Neither Secret nor Public keys were found in the input text");
    }

    Key key = new Key();
    key.setKeyData(data);
    if (data.getSecretKeyRing() != null) {
      key.setKeyInfo(buildKeyInfoFromSecret(data.getSecretKeyRing()));
    } else {
      key.setKeyInfo(buildKeyInfoFromPublic(data.getPublicKeyRing()));
    }
    return key;
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
      java.util.Date expiresAt =
          DateUtils.addSeconds(key.getCreationTime(), (int) key.getValidSeconds());
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
    for (Iterator iter = key.getSignatures(); iter.hasNext(); ) {
      PGPSignature sig = (PGPSignature) iter.next();
      return PGPUtil.getSignatureName(sig.getKeyAlgorithm(), sig.getHashAlgorithm());
    }
    return null;
  }

  protected static KeyInfo buildKeyInfoFromSecret(PGPSecretKeyRing secretKeyRing)
      throws PGPException {
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
  private static void readKeyFromStream(KeyDataPgp data, InputStream stream) throws IOException {
    PGPObjectFactory factory =
        new PGPObjectFactory(PGPUtil.getDecoderStream(stream), fingerprintCalculator);
    for (Iterator iter = factory.iterator(); iter.hasNext(); ) {
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
      if (keyDataPgp.getPublicKeyRing() != null) {
        keyDataPgp.getPublicKeyRing().encode(os.peek());
      } else {
        keyDataPgp.getSecretKeyRing().getPublicKey().encode(os.peek());
      }
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
    return baos.toString();
  }

  @Override
  public void exportPrivateKey(Key key, String targetFilePathname) {
    Preconditions.checkArgument(
        key != null && key.getKeyData() != null && key.getKeyInfo() != null,
        "Key must be providedand fully described");
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
      if (keyDataPgp.getPublicKeyRing() != null) {
        keyDataPgp.getPublicKeyRing().encode(os.peek());
      }
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
      throw new ValidationException(new ValidationError(pe.getMessageCode(), FN_PASSWORD));
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
      throw new ValidationException(
          new ValidationError(pe.getMessageCode(), CreateKeyParams.FN_PASSPHRASE));
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
