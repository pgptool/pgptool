package org.pgptool.gui.ui.embedded_viewer;

import com.google.common.base.Preconditions;
import java.util.List;
import org.pgptool.gui.filecomparison.Fingerprint;
import org.springframework.util.CollectionUtils;

public class EmbeddedViewerFactoryDelegatingImpl implements EmbeddedViewerFactory {
  private final List<EmbeddedViewerFactory> viewFactories;
  private List<String> supportedExtensions;

  public EmbeddedViewerFactoryDelegatingImpl(List<EmbeddedViewerFactory> viewFactories) {
    Preconditions.checkArgument(
        !CollectionUtils.isEmpty(viewFactories), "No view factories provided");
    this.viewFactories = viewFactories;
  }

  @Override
  public List<String> getSupportedExtensions() {
    if (supportedExtensions == null) {
      supportedExtensions =
          viewFactories.stream().flatMap(f -> f.getSupportedExtensions().stream()).toList();
    }
    return supportedExtensions;
  }

  @Override
  public boolean isExtensionSupported(String extension) {
    return viewFactories.stream().anyMatch(f -> f.isExtensionSupported(extension));
  }

  @Override
  public EmbeddedViewer build(
      String anticipatedExtension,
      byte[] bytes,
      String encryptedFilename,
      Fingerprint source,
      Fingerprint target) {
    EmbeddedViewerFactory embeddedViewerFactory =
        viewFactories.stream()
            .filter(f -> f.isExtensionSupported(anticipatedExtension))
            .findFirst()
            .orElse(null);
    Preconditions.checkArgument(
        embeddedViewerFactory != null, "No factory for extension: %s", anticipatedExtension);
    return embeddedViewerFactory.build(
        anticipatedExtension, bytes, encryptedFilename, source, target);
  }
}
