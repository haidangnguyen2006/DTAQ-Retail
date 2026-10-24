package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqIcons;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Ô tô màu bo góc hoặc hình tròn (logo, avatar, ô icon, chấm trạng thái).
 * Gom phần tự vẽ nền dùng chung để các component không lặp lại {@code paintComponent}.
 */
public class Tile extends JPanel {

  /** Dùng làm {@code arc} để vẽ hình tròn/oval. */
  public static final int OVAL = -1;

  private Color fill;
  private final int arc;

  public Tile(Color fill, int arc) {
    super(new MigLayout("fill, insets 0", "[center]", "[center]"));
    this.fill = fill;
    this.arc = arc;
    setOpaque(false);
  }

  /** Ô vuông/tròn kích thước cố định chứa một icon SVG ở giữa. */
  public static Tile icon(String iconName, int iconSize, Color iconColor, Color fill, int size, int arc) {
    Tile tile = new Tile(fill, arc);
    tile.setPreferredSize(new Dimension(size, size));
    tile.add(new JLabel(DtaqIcons.icon(iconName, iconSize, iconColor)));
    return tile;
  }

  public void setFill(Color fill) {
    this.fill = fill;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(fill);
    if (arc == OVAL) {
      g2.fillOval(0, 0, getWidth(), getHeight());
    } else {
      g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc * 2f, arc * 2f));
    }
    g2.dispose();
    super.paintComponent(g);
  }
}
