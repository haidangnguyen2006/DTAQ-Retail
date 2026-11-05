package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thông báo chỉ dẫn (Callout) với 3 mức: thông tin, cảnh báo, lỗi.
 */
public class InfoCallout extends JPanel {

  public enum Kind {
    INFO("info", DtaqColors.PRIMARY_SOFT, DtaqColors.PRIMARY, DtaqColors.PRIMARY_STRONG),
    WARNING("alert-circle", DtaqColors.WARNING_SOFT, DtaqColors.WARNING, DtaqColors.ACCENT_TEXT),
    DANGER("alert-circle", DtaqColors.DANGER_SOFT, DtaqColors.DANGER, DtaqColors.DANGER_STRONG);

    private final String icon;
    private final Color background;
    private final Color iconColor;
    private final Color textColor;

    Kind(String icon, Color background, Color iconColor, Color textColor) {
      this.icon = icon;
      this.background = background;
      this.iconColor = iconColor;
      this.textColor = textColor;
    }
  }

  private final JLabel textLabel = new JLabel();
  private final Color accent;

  public InfoCallout(String text) {
    this(Kind.INFO, text);
  }

  public InfoCallout(Kind kind, String text) {
    super(new MigLayout("fillx, insets 12 14 12 17", "[]10[grow]", "[]"));
    this.accent = kind.iconColor;
    setOpaque(true);
    setBackground(kind.background);
    putClientProperty(FlatClientProperties.STYLE, "arc: 16");

    add(new JLabel(DtaqIcons.icon(kind.icon, 17, kind.iconColor)), "top");

    textLabel.setFont(DtaqFonts.BODY_13);
    textLabel.setForeground(kind.textColor);
    setText(text);
    add(textLabel, "growx");
  }

  /** Vẽ thêm vạch nhấn 3px bên trái, cắt theo góc bo của khối. */
  @Override
  protected void paintComponent(java.awt.Graphics g) {
    super.paintComponent(g);
    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
    g2.clip(new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
    g2.setColor(accent);
    g2.fillRect(0, 0, 3, getHeight());
    g2.dispose();
  }

  public void setText(String text) {
    textLabel.setText("<html>" + (text == null ? "" : text) + "</html>");
  }
}
