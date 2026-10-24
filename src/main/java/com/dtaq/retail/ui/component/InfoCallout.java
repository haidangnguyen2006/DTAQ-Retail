package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Hộp thông báo chỉ dẫn mềm (Info Callout) với nền xanh ngọc nhạt và icon info.
 */
public class InfoCallout extends JPanel {

  public InfoCallout(String text) {
    super(new MigLayout("fillx, insets 14", "[]10[grow]", "[]"));
    setOpaque(false);

    JLabel iconLabel = new JLabel(DtaqIcons.icon("info", 17, DtaqColors.PRIMARY));
    add(iconLabel, "top");

    JLabel textLabel = new JLabel("<html>" + text + "</html>");
    textLabel.setFont(DtaqFonts.CAPTION);
    textLabel.setForeground(DtaqColors.PRIMARY_STRONG);
    add(textLabel, "growx");
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int w = getWidth() - 1;
    int h = getHeight() - 1;

    g2.setColor(DtaqColors.PRIMARY_SOFT);
    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, DtaqSpacing.RADIUS_M, DtaqSpacing.RADIUS_M));

    g2.dispose();
    super.paintComponent(g);
  }
}
