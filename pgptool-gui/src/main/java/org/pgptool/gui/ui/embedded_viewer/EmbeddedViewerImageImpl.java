package org.pgptool.gui.ui.embedded_viewer;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

class EmbeddedViewerImageImpl implements EmbeddedViewer {
  private final byte[] bytes;
  private final String encryptedFilename;
  private final String anticipatedExtension;

  public EmbeddedViewerImageImpl(
      byte[] bytes, String encryptedFilename, String anticipatedExtension) {
    this.bytes = bytes;
    this.encryptedFilename = encryptedFilename;
    this.anticipatedExtension = anticipatedExtension;
  }

  @Override
  public void present() {
    BufferedImage image = null;
    try {
      image = ImageIO.read(new ByteArrayInputStream(bytes));
    } catch (Throwable t) {
      // ignore, will handle as null
    }

    if (image == null) {
      JOptionPane.showMessageDialog(
          null, "Failed to decode image data", "Image viewer", JOptionPane.ERROR_MESSAGE);
      return;
    }

    // Create a panel that scales the image to fit the window while preserving aspect ratio
    JComponent imagePanel = new ScaledImagePanel(image);

    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.add(imagePanel, BorderLayout.CENTER);
    frame.setSize(900, 700);
    String title =
        (encryptedFilename != null && !encryptedFilename.isBlank()
                ? encryptedFilename
                : ("Decrypted image (." + anticipatedExtension + ")"))
            + String.format(" [%dx%d]", image.getWidth(), image.getHeight());
    frame.setTitle(title);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }

  private static class ScaledImagePanel extends JPanel {
    private final BufferedImage img;

    ScaledImagePanel(BufferedImage img) {
      this.img = img;
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (img == null) return;

      int iw = img.getWidth();
      int ih = img.getHeight();
      int pw = getWidth();
      int ph = getHeight();
      if (iw <= 0 || ih <= 0 || pw <= 0 || ph <= 0) return;

      double sx = (double) pw / iw;
      double sy = (double) ph / ih;
      double s = Math.min(sx, sy);

      int dw = Math.max(1, (int) Math.round(iw * s));
      int dh = Math.max(1, (int) Math.round(ih * s));

      int dx = (pw - dw) / 2;
      int dy = (ph - dh) / 2;

      Graphics2D g2 = (Graphics2D) g.create();
      try {
        g2.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(img, dx, dy, dw, dh, null);
      } finally {
        g2.dispose();
      }
    }
  }
}
