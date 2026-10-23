package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
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
 * Mục hoạt động / nhật ký / cảnh báo (ActivityItem) có ô icon 36x36 bo góc r8 bên trái.
 */
public class ActivityItem extends JPanel {

  public ActivityItem(String iconName, boolean isDanger, String titleText, String subtitleText) {
    super(new MigLayout("fillx, insets 0", "[]12[grow]", "[]"));
    setOpaque(false);

    Color bgColor = isDanger ? DtaqColors.DANGER_SOFT_2 : DtaqColors.PRIMARY_SOFT;
    Color iconColor = isDanger ? DtaqColors.DANGER : DtaqColors.PRIMARY;

    JPanel iconTile = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bgColor);
        g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_CARD_SM, DtaqSpacing.RADIUS_CARD_SM));
        g2.dispose();
        super.paintComponent(g);
      }
    };
    iconTile.setOpaque(false);
    iconTile.setPreferredSize(new Dimension(36, 36));
    iconTile.setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
    iconTile.add(new JLabel(DtaqIcons.icon(iconName, 18, iconColor)));

    add(iconTile, "top");

    JPanel textBlock = new JPanel(new MigLayout("insets 0", "[]", "[]2[]"));
    textBlock.setOpaque(false);

    JLabel title = new JLabel(titleText);
    title.setFont(DtaqFonts.BODY_13);
    title.setForeground(isDanger ? DtaqColors.DANGER_STRONG : DtaqColors.TEXT);
    textBlock.add(title, "wrap");

    if (subtitleText != null && !subtitleText.trim().isEmpty()) {
      JLabel sub = new JLabel(subtitleText);
      sub.setFont(DtaqFonts.CAPTION);
      sub.setForeground(isDanger ? DtaqColors.DANGER : DtaqColors.TEXT_MUTED);
      textBlock.add(sub, "wrap");
    }

    add(textBlock, "growx");
  }
}
