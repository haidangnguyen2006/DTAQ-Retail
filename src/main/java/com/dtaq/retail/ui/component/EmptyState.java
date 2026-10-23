package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Khối hiển thị trạng thái rỗng (Empty State) theo thiết kế Figma.
 */
public class EmptyState extends JPanel {

  public EmptyState(String iconName, String titleText, String hintText) {
    super(new MigLayout("fill, insets 32", "[center]", "[]12[]6[]"));
    setOpaque(false);

    JPanel iconCircle = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(DtaqColors.BG_APP);
        g2.fillOval(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
      }
    };
    iconCircle.setOpaque(false);
    iconCircle.setPreferredSize(new Dimension(46, 46));
    iconCircle.setLayout(new MigLayout("fill, insets 0", "[center]", "[center]"));
    iconCircle.add(new JLabel(DtaqIcons.icon(iconName, 23, DtaqColors.TEXT_SUBTLE)));

    add(iconCircle, "center, wrap");

    JLabel title = new JLabel(titleText);
    title.setFont(DtaqFonts.EMPTY_TITLE);
    title.setForeground(DtaqColors.TEXT);
    add(title, "center, wrap");

    if (hintText != null && !hintText.trim().isEmpty()) {
      JLabel hint = new JLabel(hintText);
      hint.setFont(DtaqFonts.CAPTION);
      hint.setForeground(DtaqColors.TEXT_MUTED);
      add(hint, "center");
    }
  }
}
