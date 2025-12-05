package org.pgptool.gui.ui.embedded_viewer;

import static org.pgptool.gui.tools.osnative.OsNativeApiResolver.isWindows;

import java.awt.BorderLayout;
import java.awt.Font;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.pgptool.gui.tools.osnative.OsNativeApi;
import org.pgptool.gui.tools.osnative.OsNativeApiResolver;

class EmbeddedViewerTextImpl implements EmbeddedViewer {
  private final OsNativeApi osNativeApi;

  private final byte[] bytes;
  private final String encryptedFilename;
  private final String anticipatedExtension;

  public EmbeddedViewerTextImpl(
      byte[] bytes, String encryptedFilename, String anticipatedExtension) {
    this.osNativeApi = OsNativeApiResolver.resolve();
    this.bytes = bytes;
    this.encryptedFilename = encryptedFilename;
    this.anticipatedExtension = anticipatedExtension;
  }

  @Override
  public void present() {
    String text = decodeBestEffort(bytes);

    JTextArea textArea = new JTextArea(text);
    // NOTE: Later on we might want to add a text editor which will allow modifying text in memory
    // and
    // encrypt back. But this is going to be a separate feature as it requires significant work due
    // to implicit user expectations, like all hot keys and even find feature
    textArea.setEditable(false);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    // Monospaced font is often better for plain text files
    textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));

    JScrollPane scrollPane = new JScrollPane(textArea);

    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(scrollPane, BorderLayout.CENTER);
    frame.setSize(800, 600);
    String title =
        encryptedFilename != null && !encryptedFilename.isBlank()
            ? encryptedFilename
            : ("Decrypted text (." + anticipatedExtension + ")");
    frame.setTitle(title);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private String decodeBestEffort(byte[] data) {
    if (data == null || data.length == 0) {
      return "";
    }

    // 1) BOM detection
    BomDetection bom = detectBom(data);
    if (bom != null) {
      try {
        return new String(data, bom.offset, data.length - bom.offset, bom.charset);
      } catch (Throwable ignored) {
        // fall through
      }
    }

    // 2) Strict UTF-8 validation
    if (isValidUtf8(data)) {
      return new String(data, StandardCharsets.UTF_8);
    }

    // 3) On Windows, try system ANSI code page (Language for non-Unicode programs)
    String acpText = tryDecodeWithWindowsAcp(data);
    if (acpText != null) {
      return acpText;
    }

    // 4) Fallback to system default
    try {
      return new String(data, Charset.defaultCharset());
    } catch (Throwable t) {
      // last resort
      return new String(data, StandardCharsets.UTF_8);
    }
  }

  private static boolean isValidUtf8(byte[] data) {
    try {
      StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(data));
      return true;
    } catch (CharacterCodingException ex) {
      return false;
    } catch (Throwable t) {
      // On unexpected failures be conservative
      return false;
    }
  }

  private String tryDecodeWithWindowsAcp(byte[] data) {
    try {
      if (!isWindows()) {
        return null;
      }

      Charset cs = osNativeApi.findDefaultCharset();
      if (cs == null) {
        return null;
      }

      String s = new String(data, cs);
      // Basic plausibility: avoid lots of replacement characters and controls
      double score = scoreTextPlausibility(s);
      if (score >= 0.85) {
        return s;
      }
    } catch (Throwable ignored) {
      // ignore and fall through
    }
    return null;
  }

  private static double scoreTextPlausibility(String s) {
    if (s == null || s.isEmpty()) {
      return 0.0;
    }
    int total = s.length();
    int bad = 0;
    for (int i = 0; i < total; i++) {
      char ch = s.charAt(i);
      if (ch == '\uFFFD'
          || (Character.isISOControl(ch) && ch != '\n' && ch != '\r' && ch != '\t')) {
        bad++;
      }
    }
    double badRatio = (double) bad / (double) total;
    return 1.0 - Math.min(1.0, badRatio);
  }

  private static class BomDetection {
    final Charset charset;
    final int offset;

    BomDetection(Charset charset, int offset) {
      this.charset = charset;
      this.offset = offset;
    }
  }

  private static BomDetection detectBom(byte[] data) {
    if (data.length >= 3
        && (data[0] & 0xFF) == 0xEF
        && (data[1] & 0xFF) == 0xBB
        && (data[2] & 0xFF) == 0xBF) {
      return new BomDetection(StandardCharsets.UTF_8, 3);
    }
    if (data.length >= 2) {
      int b0 = data[0] & 0xFF;
      int b1 = data[1] & 0xFF;
      if (b0 == 0xFE && b1 == 0xFF) {
        return new BomDetection(StandardCharsets.UTF_16BE, 2);
      }
      if (b0 == 0xFF && b1 == 0xFE) {
        return new BomDetection(StandardCharsets.UTF_16LE, 2);
      }
    }
    if (data.length >= 4) {
      int b0 = data[0] & 0xFF;
      int b1 = data[1] & 0xFF;
      int b2 = data[2] & 0xFF;
      int b3 = data[3] & 0xFF;
      if (b0 == 0x00 && b1 == 0x00 && b2 == 0xFE && b3 == 0xFF) {
        return new BomDetection(Charset.forName("UTF-32BE"), 4);
      }
      if (b0 == 0xFF && b1 == 0xFE && b2 == 0x00 && b3 == 0x00) {
        return new BomDetection(Charset.forName("UTF-32LE"), 4);
      }
    }
    return null;
  }
}
