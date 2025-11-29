package org.pgptool.gui.encryption.implpgp;

import static org.junit.Assert.assertEquals;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.junit.Test;

public class KeyGeneratorServicePgpImplTest {

  @Test
  public void testAlgorithmNameToTag() {
    assertEquals(PGPPublicKey.DSA, KeyGeneratorServicePgpImpl.algorithmNameToTag("DSA"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAlgorithmNameToTagExpectFail() {
    KeyGeneratorServicePgpImpl.algorithmNameToTag("invalid");
  }
}
