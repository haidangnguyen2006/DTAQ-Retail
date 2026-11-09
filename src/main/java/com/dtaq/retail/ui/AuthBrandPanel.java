package com.dtaq.retail.ui;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/** Panel thương hiệu tối màu dùng chung cho màn hình Đăng nhập và Quên mật khẩu. */
final class AuthBrandPanel extends JPanel {

  private static final int GRID = 24;

  /** Hoa văn đã vẽ sẵn theo kích thước hiện tại, để mỗi lần repaint chỉ tốn một lệnh drawImage. */
  private BufferedImage cache;

  AuthBrandPanel() {
    super(new MigLayout("fill, insets 0", "[center]", "[center]"));
    setBackground(DtaqColors.BRAND_DARK);

    JPanel brandBlock = new JPanel(new MigLayout("insets 0", "[center]", "[]16[]"));
    brandBlock.setOpaque(false);

    JLabel logo = new JLabel(com.dtaq.retail.ui.theme.DtaqTheme.logo(88));
    logo.setToolTipText("DTAQ");
    brandBlock.add(logo, "center, wrap");

    JLabel description = new JLabel("Quản lý siêu thị điện máy");
    description.setFont(DtaqFonts.BODY_14);
    description.setForeground(DtaqColors.BRAND_ON_DARK_MUTED);
    brandBlock.add(description, "center");

    add(brandBlock, "center");
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    int w = getWidth();
    int h = getHeight();
    if (w <= 0 || h <= 0) {
      return;
    }
    if (cache == null || cache.getWidth() != w || cache.getHeight() != h) {
      cache = render(w, h);
    }
    g.drawImage(cache, 0, 0, null);
  }

  /** Lưới chấm mờ, vài đường "mạch điện" gấp khúc 90° và vầng sáng ở góc dưới trái. */
  private static BufferedImage render(int w, int h) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = img.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

    float radius = Math.max(w, h) * 0.75f;
    g2.setPaint(new RadialGradientPaint(0, h, radius, new float[] {0f, 1f},
        new java.awt.Color[] {DtaqColors.alpha(DtaqColors.PRIMARY, 0.28f), DtaqColors.alpha(DtaqColors.PRIMARY, 0f)}));
    g2.fillRect(0, 0, w, h);

    g2.setColor(DtaqColors.alpha(DtaqColors.SURFACE, 0.06f));
    for (int x = GRID; x < w; x += GRID) {
      for (int y = GRID; y < h; y += GRID) {
        g2.fillOval(x - 1, y - 1, 3, 3);
      }
    }

    g2.setColor(DtaqColors.alpha(DtaqColors.PRIMARY, 0.35f));
    g2.setStroke(new java.awt.BasicStroke(1.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
    int[][] traces = {
        {GRID * 2, GRID * 3, GRID * 6, GRID * 3, GRID * 6, GRID * 6, GRID * 9, GRID * 6},
        {w - GRID * 2, GRID * 5, w - GRID * 5, GRID * 5, w - GRID * 5, GRID * 9, w - GRID * 8, GRID * 9},
        {GRID * 3, h - GRID * 4, GRID * 7, h - GRID * 4, GRID * 7, h - GRID * 7, GRID * 10, h - GRID * 7},
    };
    for (int[] t : traces) {
      java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
      path.moveTo(t[0], t[1]);
      for (int i = 2; i < t.length; i += 2) {
        path.lineTo(t[i], t[i + 1]);
      }
      g2.draw(path);
      g2.fillOval(t[0] - 3, t[1] - 3, 7, 7);
      g2.fillOval(t[t.length - 2] - 3, t[t.length - 1] - 3, 7, 7);
    }
    g2.dispose();
    return img;
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(0, 0);
  }
}
