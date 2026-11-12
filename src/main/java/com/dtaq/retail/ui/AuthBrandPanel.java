package com.dtaq.retail.ui;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/** Panel thương hiệu tối màu dùng chung cho màn hình Đăng nhập và Quên mật khẩu. */
final class AuthBrandPanel extends JPanel {

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

  /**
   * "Quỹ đạo DTAQ": hai cung elip mảnh nghiêng, lấy từ nét vòng cung trong logo, nằm sau logo.
   * Chỉ xuất hiện ở panel thương hiệu này — không gradient, không lưới chấm, không đường mạch.
   */
  private static BufferedImage render(int w, int h) {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = img.createGraphics();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    double cx = w / 2.0;
    double cy = h * 0.52;
    g2.rotate(Math.toRadians(-12), cx, cy);
    g2.setColor(DtaqColors.alpha(DtaqColors.BRAND, 0.55f));
    g2.setStroke(new java.awt.BasicStroke(2f));
    g2.draw(new java.awt.geom.Ellipse2D.Double(cx - w * 0.78, cy - h * 0.20, w * 1.56, h * 0.40));
    g2.setColor(DtaqColors.alpha(DtaqColors.BRAND_400, 0.22f));
    g2.setStroke(new java.awt.BasicStroke(1.25f));
    g2.draw(new java.awt.geom.Ellipse2D.Double(cx - w * 0.98, cy - h * 0.31, w * 1.96, h * 0.62));
    g2.dispose();
    return img;
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(0, 0);
  }
}
