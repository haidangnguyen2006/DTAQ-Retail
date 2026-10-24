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
    NEUTRAL(DtaqColors.BG_APP, DtaqColors.TEXT_MUTED),
    SUCCESS(DtaqColors.PRIMARY_SOFT, DtaqColors.PRIMARY_STRONG),
    DANGER(DtaqColors.DANGER_SOFT, DtaqColors.DANGER);

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
  }

  public static StatusPill neutral(String text) {
    return new StatusPill(text, Type.NEUTRAL);
  }

  public static StatusPill success(String text) {
    return new StatusPill(text, Type.SUCCESS);
  }

  public static StatusPill danger(String text) {
    return new StatusPill(text, Type.DANGER);
  }

  public void setType(Type type) {
    this.type = type;
    setForeground(type.foreground);
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    return new Dimension(d.width + 18, DtaqSpacing.PILL_H);
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int w = getWidth();
    int h = getHeight();

    g2.setColor(type.background);
    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, h, h));

    g2.dispose();
    super.paintComponent(g);
  }
}
