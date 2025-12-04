package org.pgptool.gui.ui.embedded_viewer;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.pgptool.gui.filecomparison.Fingerprint;

/** Embedded viewer factory capable of displaying images supported by standard Java ImageIO. */
public class EmbeddedViewerFactoryImageImpl implements EmbeddedViewerFactory {
  private static final Set<String> SUPPORTED_EXTENSIONS =
      new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "wbmp"));

  @Override
  public boolean isExtensionSupported(String extension) {
    if (extension == null) return false;
    String ext = extension.toLowerCase(Locale.ROOT);
    return SUPPORTED_EXTENSIONS.contains(ext);
  }

  @Override
  public List<String> getSupportedExtensions() {
    return new ArrayList<>(SUPPORTED_EXTENSIONS);
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
        "Extension '%s' is not supported by EmbeddedViewerFactoryImageImpl",
        anticipatedExtension);

    return new EmbeddedViewerImageImpl(bytes, encryptedFilename, anticipatedExtension);
  }
}
