package org.pgptool.gui.encryption.implpgp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.junit.jupiter.api.Test;

public class KeyGeneratorServicePgpImplTest {

  @Test
  public void testAlgorithmNameToTag() {
    assertEquals(PGPPublicKey.DSA, KeyGeneratorServicePgpImpl.algorithmNameToTag("DSA"));
  }

  @Test
  public void testAlgorithmNameToTagExpectFail() {
    assertThrows(
        IllegalArgumentException.class,
        () -> KeyGeneratorServicePgpImpl.algorithmNameToTag("invalid"));
  }
}
