package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JButton;

/**
 * Nút bấm chuẩn theo hệ thống Design Token DTAQ.
 * Hỗ trợ các biến thể: Primary, Secondary, Danger, Small, Segmented, Link.
 */
public class DtaqButton extends JButton {

  public enum Variant {
    PRIMARY,
    SECONDARY,
    DANGER_SOFT,
    SEGMENTED_ACTIVE,
    SEGMENTED_INACTIVE,
    LINK
  }

  private final Variant variant;
  private String shortcutKey;

  public DtaqButton(String text, Variant variant) {
    super(text);
    this.variant = variant;
    initStyle();
  }

  public static DtaqButton primary(String text) {
    return new DtaqButton(text, Variant.PRIMARY);
  }

  public static DtaqButton primary(String text, String iconName) {
    DtaqButton btn = new DtaqButton(text, Variant.PRIMARY);
    if (iconName != null) {
      btn.setIcon(DtaqIcons.icon(iconName, 17, Color.WHITE));
      btn.setIconTextGap(10);
    }
    return btn;
  }

  public static DtaqButton secondary(String text) {
    return new DtaqButton(text, Variant.SECONDARY);
  }

  public static DtaqButton secondary(String text, String iconName) {
    DtaqButton btn = new DtaqButton(text, Variant.SECONDARY);
    if (iconName != null) {
      btn.setIcon(DtaqIcons.icon(iconName, 17, DtaqColors.TEXT_MUTED));
      btn.setIconTextGap(8);
    }
    return btn;
  }

  public static DtaqButton dangerSoft(String text) {
    return new DtaqButton(text, Variant.DANGER_SOFT);
  }

  public static DtaqButton primarySmall(String text) {
    DtaqButton btn = new DtaqButton(text, Variant.PRIMARY);
    btn.setFont(DtaqFonts.SMALL_BUTTON);
    btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, DtaqSpacing.BUTTON_H_SM));
    return btn;
  }

  public static DtaqButton dangerSmall(String text) {
    DtaqButton btn = new DtaqButton(text, Variant.DANGER_SOFT);
    btn.setFont(DtaqFonts.SMALL_BUTTON);
    btn.setPreferredSize(new Dimension(btn.getPreferredSize().width, DtaqSpacing.BUTTON_H_SM));
    return btn;
  }

  public static DtaqButton link(String text) {
    return new DtaqButton(text, Variant.LINK);
  }

  public void setShortcutKey(String key) {
    this.shortcutKey = key;
    repaint();
  }

  private void initStyle() {
    setFocusPainted(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
    putClientProperty(FlatClientProperties.STYLE, "arc: " + DtaqSpacing.RADIUS_M);

    switch (variant) {
      case PRIMARY -> {
        setFont(DtaqFonts.BUTTON);
        setBackground(DtaqColors.PRIMARY);
        setForeground(Color.WHITE);
        putClientProperty(FlatClientProperties.STYLE,
            "background: #087F75; foreground: #FFFFFF; borderWidth: 0; arc: 6");
        setPreferredSize(new Dimension(getPreferredSize().width, DtaqSpacing.BUTTON_H));
      }
      case SECONDARY -> {
        setFont(DtaqFonts.BUTTON);
        setBackground(DtaqColors.SURFACE);
        setForeground(DtaqColors.TEXT);
        putClientProperty(FlatClientProperties.STYLE,
            "background: #FFFFFF; foreground: #18252D; borderColor: #E2E8EC; borderWidth: 1; arc: 6");
        setPreferredSize(new Dimension(getPreferredSize().width, DtaqSpacing.BUTTON_H));
      }
      case DANGER_SOFT -> {
        setFont(DtaqFonts.SMALL_BUTTON);
        setBackground(DtaqColors.DANGER_SOFT);
        setForeground(DtaqColors.DANGER);
        putClientProperty(FlatClientProperties.STYLE,
            "background: #FEE4E2; foreground: #B42318; borderWidth: 0; arc: 6");
        setPreferredSize(new Dimension(getPreferredSize().width, DtaqSpacing.BUTTON_H_SM));
      }
      case SEGMENTED_ACTIVE -> {
        setFont(DtaqFonts.SMALL_BUTTON);
        setBackground(DtaqColors.PRIMARY_SOFT);
        setForeground(DtaqColors.PRIMARY_STRONG);
        putClientProperty(FlatClientProperties.STYLE,
            "background: #E8F5F2; foreground: #075E57; borderWidth: 0; arc: 6");
        setPreferredSize(new Dimension(getPreferredSize().width, DtaqSpacing.BUTTON_H_SM));
      }
      case SEGMENTED_INACTIVE -> {
        setFont(DtaqFonts.SMALL_BUTTON);
        setBackground(DtaqColors.SURFACE);
        setForeground(DtaqColors.TEXT_MUTED);
        putClientProperty(FlatClientProperties.STYLE,
            "background: #FFFFFF; foreground: #667580; borderColor: #E2E8EC; borderWidth: 1; arc: 6");
        setPreferredSize(new Dimension(getPreferredSize().width, DtaqSpacing.BUTTON_H_SM));
      }
      case LINK -> {
        setFont(DtaqFonts.CAPTION);
        setForeground(DtaqColors.PRIMARY);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
      }
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (shortcutKey != null && !shortcutKey.isEmpty() && isVisible()) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setFont(DtaqFonts.HINT_ON_PRIMARY);
      if (isEnabled()) {
        g2.setColor(variant == Variant.PRIMARY ? DtaqColors.PRIMARY_SOFT_2 : DtaqColors.TEXT_MUTED);
      } else {
        g2.setColor(DtaqColors.TEXT_SUBTLE);
      }
      int textWidth = g2.getFontMetrics().stringWidth(shortcutKey);
      int x = getWidth() - textWidth - 14;
      int y = (getHeight() + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
      g2.drawString(shortcutKey, x, y);
      g2.dispose();
    }
  }
}
