package org.pgptool.gui.ui.embedded_viewer;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EmbeddedViewerPdfImpl implements EmbeddedViewer {
  private static final Logger log = LoggerFactory.getLogger(EmbeddedViewerPdfImpl.class);

  private final String encryptedFilename;
  private final String anticipatedExtension;
  private final byte[] bytes;

  public EmbeddedViewerPdfImpl(
      String encryptedFilename, String anticipatedExtension, byte[] bytes) {
    this.encryptedFilename = encryptedFilename;
    this.anticipatedExtension = anticipatedExtension;
    this.bytes = bytes;
  }

  @Override
  public void present() {
    try {
      // Create PDFBox-based viewer UI
      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setLayout(new BorderLayout());
      frame.setSize(1200, 700);
      String title =
          encryptedFilename != null && !encryptedFilename.isBlank()
              ? encryptedFilename
              : ("Decrypted PDF (." + anticipatedExtension + ")");
      frame.setTitle(title);

      // Load PDDocument from memory
      final PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes));

      // Render pages to images and put into a scrollable panel
      JPanel pagesPanel = new JPanel();
      pagesPanel.setLayout(new BoxLayout(pagesPanel, BoxLayout.Y_AXIS));
      PDFRenderer renderer = new PDFRenderer(document);
      // Compute target width to fit page images to frame content width
      int targetWidthPx = Math.max(600, frame.getWidth() - 100); // account for margins/scrollbar
      for (int i = 0; i < document.getNumberOfPages(); i++) {
        // Calculate scale so that page image fits by width
        float pageWidthPts = document.getPage(i).getMediaBox().getWidth(); // points at 72 DPI
        // At scale=1, renderImage returns image where width ~ pageWidthPts (since 72dpi)
        float scale = pageWidthPts > 0 ? (float) targetWidthPx / pageWidthPts : 1.0f;
        if (scale > 2.0f) {
          // avoid excessive upscaling that hurts quality/memory
          scale = 2.0f;
        }
        BufferedImage image = renderer.renderImage(i, scale);
        JLabel pageLabel = new JLabel(new ImageIcon(image));
        pageLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        pageLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pagesPanel.add(pageLabel);
      }

      JScrollPane scrollPane = new JScrollPane(pagesPanel);
      scrollPane.getVerticalScrollBar().setUnitIncrement(16);
      frame.add(scrollPane, BorderLayout.CENTER);

      // Ensure resources are disposed when window closes
      frame.addWindowListener(
          new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
              try {
                document.close();
              } catch (IOException ex) {
                log.warn("Failed to close PDF document", ex);
              }
            }
          });

      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    } catch (Throwable t) {
      throw new RuntimeException("Failed to render PDF document", t);
    }
  }
}
