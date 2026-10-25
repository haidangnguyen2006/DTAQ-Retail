package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Mục hoạt động / nhật ký / cảnh báo (ActivityItem) có ô icon 36x36 bo góc bên trái.
 */
public class ActivityItem extends JPanel {

  public ActivityItem(String iconName, boolean isDanger, String titleText, String subtitleText) {
    super(new MigLayout("fillx, insets 0", "[]12[grow]", "[]"));
    setOpaque(false);

    Color bgColor = isDanger ? DtaqColors.DANGER_SOFT_2 : DtaqColors.PRIMARY_SOFT;
    Color iconColor = isDanger ? DtaqColors.DANGER : DtaqColors.PRIMARY;
    add(Tile.icon(iconName, 18, iconColor, bgColor, 36, DtaqSpacing.RADIUS_CARD_SM), "top");

    JPanel textBlock = new JPanel(new MigLayout("insets 0, gapy 2", "[]", "[]"));
    textBlock.setOpaque(false);

    JLabel title = new JLabel(titleText);
    title.setFont(DtaqFonts.BODY_13_MEDIUM);
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
