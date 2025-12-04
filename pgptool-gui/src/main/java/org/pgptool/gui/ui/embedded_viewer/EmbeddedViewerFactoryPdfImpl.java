package org.pgptool.gui.ui.embedded_viewer;

import com.google.common.base.Preconditions;
import java.util.List;
import org.pgptool.gui.filecomparison.Fingerprint;

/** Embedded viewer factory based on Apache PDFBox to display PDF files. */
public class EmbeddedViewerFactoryPdfImpl implements EmbeddedViewerFactory {
  public static final String PDF = "pdf";

  @Override
  public boolean isExtensionSupported(String extension) {
    return PDF.equalsIgnoreCase(extension);
  }

  @Override
  public List<String> getSupportedExtensions() {
    return List.of(PDF);
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
        "Extension '%s' is not supported by EmbeddedViewerFactoryPdfImpl",
        anticipatedExtension);

    return new EmbeddedViewerPdfImpl(encryptedFilename, anticipatedExtension, bytes);
  }
}
