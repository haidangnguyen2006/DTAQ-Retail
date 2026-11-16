package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Huy hiệu trạng thái (Status Pill) bo tròn hoàn toàn theo design tokens.
 */
public class StatusPill extends JLabel {

  public enum Type {
    NEUTRAL(DtaqColors.DISABLED_BG, DtaqColors.PILL_NEUTRAL_TEXT),
    SUCCESS(DtaqColors.SUCCESS_SOFT, DtaqColors.SUCCESS),
    WARNING(DtaqColors.WARNING_SOFT, DtaqColors.WARNING),
    DANGER(DtaqColors.DANGER_SOFT, DtaqColors.DANGER_STRONG),
    INFO(DtaqColors.PRIMARY_SOFT, DtaqColors.PRIMARY_STRONG);

    final Color background;
    final Color foreground;

    Type(Color background, Color foreground) {
      this.background = background;
      this.foreground = foreground;
    }
  }

  private Type type;

  public StatusPill(String text, Type type) {
    super(text, SwingConstants.CENTER);
    this.type = type;
    setFont(DtaqFonts.PILL);
    setForeground(type.foreground);
    setOpaque(false);
    applyDotSpace();
  }

  /** Pill trung tính (ngày tháng, số đếm) không mang trạng thái nên không vẽ chấm và không chừa chỗ. */
  private boolean hasDot() {
    return type != Type.NEUTRAL;
  }

  private void applyDotSpace() {
    setBorder(javax.swing.BorderFactory.createEmptyBorder(0, hasDot() ? DOT_SPACE : 0, 0, 0));
  }

  /** Chỗ chừa cho chấm tròn đầu pill (lề trái + đường kính chấm + khoảng cách tới chữ). */
  private static final int DOT_SPACE = 10;
  private static final float DOT_SIZE = 6f;

  public static StatusPill neutral(String text) {
    return new StatusPill(text, Type.NEUTRAL);
  }

  public static StatusPill success(String text) {
    return new StatusPill(text, Type.SUCCESS);
  }

  public static StatusPill warning(String text) {
    return new StatusPill(text, Type.WARNING);
  }

  public static StatusPill danger(String text) {
    return new StatusPill(text, Type.DANGER);
  }

  public static StatusPill info(String text) {
    return new StatusPill(text, Type.INFO);
  }

  public void setType(Type type) {
    this.type = type;
    setForeground(type.foreground);
    applyDotSpace();
    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    return new Dimension(Math.max(d.width + 18, 44), DtaqSpacing.PILL_H);
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int w = getWidth();
    int h = getHeight();

    g2.setColor(type.background);
    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));
    if (hasDot()) {
      g2.setColor(type.foreground);
      g2.fill(new java.awt.geom.Ellipse2D.Float(10f, (h - DOT_SIZE) / 2f, DOT_SIZE, DOT_SIZE));
    }

    g2.dispose();
    super.paintComponent(g);
  }
}
