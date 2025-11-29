package org.pgptool.gui.ui.encryptone;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class EncryptOnePmTest {

  @Test
  public void testMadeUpTargetFileNameExpectExtensionWillBeReplaced() {
    String madeUpFileName = EncryptOnePm.makeUpTargetFileName("File.ext1", "/some/path");
    assertEquals("/some/path/File.pgp", madeUpFileName);
  }

  @Test
  public void testMadeUpTargetFileNameFix228ExpectOnlyLastExtensionWillBeReplaced() {
    String madeUpFileName = EncryptOnePm.makeUpTargetFileName("File.ext1.ext2", "/some/path");
    assertEquals("/some/path/File.ext1.pgp", madeUpFileName);
  }

  @Test
  public void testMadeUpTargetFileNameFix228ExpectCorrectBehaviorForThisWeirdCase() {
    String madeUpFileName =
        EncryptOnePm.makeUpTargetFileName("2222 SOME NAME_OTHER.AAA#1111.pdf", "/some/path");
    assertEquals("/some/path/2222 SOME NAME_OTHER.AAA#1111.pgp", madeUpFileName);
  }
}
