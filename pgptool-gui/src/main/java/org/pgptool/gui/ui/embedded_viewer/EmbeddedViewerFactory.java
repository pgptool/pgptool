package org.pgptool.gui.ui.embedded_viewer;

import java.util.List;
import org.pgptool.gui.filecomparison.Fingerprint;

public interface EmbeddedViewerFactory {
  List<String> getSupportedExtensions();

  boolean isExtensionSupported(String extension);

  /**
   * @return EmbeddedViewer which presumably capable of rendering decrypted contents represented by
   *     bytes
   */
  EmbeddedViewer build(
      String anticipatedExtension,
      byte[] bytes,
      String encryptedFilename,
      Fingerprint source,
      Fingerprint target);
}
