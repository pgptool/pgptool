package org.pgptool.gui.ui.embedded_viewer;

import com.google.common.base.Preconditions;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.pgptool.gui.filecomparison.Fingerprint;
import org.springframework.util.StringUtils;

public class EmbeddedViewerFactoryTextImpl implements EmbeddedViewerFactory {
  // Broad, but safe, list of common plaintext extensions we can display as text
  private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt");

  @Override
  public List<String> getSupportedExtensions() {
    return new ArrayList<>(SUPPORTED_EXTENSIONS);
  }

  @Override
  public boolean isExtensionSupported(String extension) {
    if (!StringUtils.hasText(extension)) {
      return false;
    }

    if (SUPPORTED_EXTENSIONS.contains(extension)) {
      return true;
    }

    // Heuristic via MIME guess: treat any text/* as text; also some application/* widely text-based
    try {
      String mime = URLConnection.guessContentTypeFromName("x." + extension);
      if (mime != null) {
        if (mime.startsWith("text/")) {
          return true;
        }
      }
    } catch (Throwable ignored) {
      // Fallback to false below
    }

    return false;
  }

  @Override
  public EmbeddedViewer build(
      String anticipatedExtension,
      byte[] bytes,
      String encryptedFilename,
      Fingerprint source,
      Fingerprint target) {
    Preconditions.checkArgument(
        isExtensionSupported(anticipatedExtension),
        "Extension '%s' is not supported by EmbeddedViewerFactoryTextImpl",
        anticipatedExtension);

    // Provide a simple text viewer implementation that renders given bytes as text
    return new EmbeddedViewerTextImpl(bytes, encryptedFilename, anticipatedExtension);
  }
}
