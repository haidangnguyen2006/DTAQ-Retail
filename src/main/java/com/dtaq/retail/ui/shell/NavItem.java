package com.dtaq.retail.ui.shell;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Mục điều hướng trên thanh Sidebar (NavItem).
 */
public class NavItem extends JPanel {

  private final String iconName;
  private final String labelText;
  private final JLabel iconLabel = new JLabel();
  private final JLabel textLabel = new JLabel();

  private boolean active = false;
  private boolean hovered = false;
  private Runnable onClick;

  public NavItem(String iconName, String labelText) {
    super(new MigLayout("fillx, insets 0 12 0 12", "[]10[grow]", "[center]"));
    this.iconName = iconName;
    this.labelText = labelText;
    setOpaque(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    boolean multiline = labelText.length() > 20 || labelText.contains("\n");
    int h = multiline ? 64 : DtaqSpacing.NAV_ITEM_H;
    setPreferredSize(new Dimension(192, h));

    textLabel.setText("<html>" + labelText.replace("\n", "<br>") + "</html>");
    add(iconLabel, "align left, aligny center");
    add(textLabel, "growx, aligny center");

    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        hovered = true;
        repaint();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        hovered = false;
        repaint();
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (onClick != null) {
          onClick.run();
        }
      }
    });

    updateState();
  }

  public void setActive(boolean active) {
    this.active = active;
    updateState();
    repaint();
  }

  public boolean isActive() {
    return active;
  }

  public void setOnClick(Runnable onClick) {
    this.onClick = onClick;
  }

  private void updateState() {
    if (active) {
      iconLabel.setIcon(DtaqIcons.icon(iconName, 19, DtaqColors.PRIMARY));
      textLabel.setFont(DtaqFonts.BODY_13_MEDIUM);
      textLabel.setForeground(DtaqColors.PRIMARY_STRONG);
    } else {
      iconLabel.setIcon(DtaqIcons.icon(iconName, 19, DtaqColors.TEXT_MUTED));
      textLabel.setFont(DtaqFonts.BODY_13);
      textLabel.setForeground(DtaqColors.TEXT_MUTED);
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (active || hovered) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(active ? DtaqColors.PRIMARY_SOFT : DtaqColors.BG_APP);
      g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), DtaqSpacing.RADIUS_M, DtaqSpacing.RADIUS_M));
      g2.dispose();
    }
    super.paintComponent(g);
  }
}
