package com.dtaq.retail.ui.shell;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.ui.theme.DtaqStyles;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.RoundRectangle2D;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * Mục điều hướng trên thanh Sidebar. Là nút thật nên chọn được bằng Tab và kích hoạt bằng Enter/Space.
 */
public class NavItem extends JButton {

  private static final int ICON_SIZE = 19;
  private static final int ACCENT_WIDTH = 3;

  private final String iconName;
  private boolean active;

  public NavItem(String iconName, String labelText) {
    super(labelText);
    this.iconName = iconName;
    setHorizontalAlignment(SwingConstants.LEFT);
    setIconTextGap(12);
    setFocusPainted(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "dtaq.activate");
    getActionMap().put("dtaq.activate", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        doClick();
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
    addActionListener(e -> onClick.run());
  }

  private void updateState() {
    setFont(active ? DtaqFonts.BODY_13_MEDIUM : DtaqFonts.BODY_13);
    setIcon(DtaqIcons.icon(iconName, ICON_SIZE, active ? DtaqColors.PRIMARY : DtaqColors.TEXT_MUTED));
    putClientProperty(FlatClientProperties.STYLE, DtaqStyles.navItem(active));
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    d.height = Math.max(d.height, DtaqSpacing.NAV_ITEM_H);
    return d;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (active) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(DtaqColors.PRIMARY);
      int barHeight = getHeight() - 16;
      g2.fill(new RoundRectangle2D.Float(0, 8, ACCENT_WIDTH, barHeight, ACCENT_WIDTH, ACCENT_WIDTH));
      g2.dispose();
    }
  }
}
