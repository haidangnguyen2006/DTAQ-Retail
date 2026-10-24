package com.dtaq.retail.ui.component;

import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqFonts;
import com.dtaq.retail.ui.theme.DtaqIcons;
import com.dtaq.retail.ui.theme.DtaqSpacing;
import com.dtaq.retail.ui.theme.DtaqStyles;
import com.formdev.flatlaf.FlatClientProperties;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JButton;

/**
 * Nút bấm chuẩn theo hệ thống Design Token DTAQ.
 * Hỗ trợ các biến thể: Primary, Secondary, Danger, Segmented, Link; kích thước thường/nhỏ;
 * trạng thái bận ({@link #setBusy}) và nhãn phím tắt ({@link #setShortcutKey}).
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

  private static final String BUSY_TEXT = "Đang xử lý…";
  private static final int KBD_PADDING = 6;
  private static final int KBD_GAP = 10;

  private Variant variant;
  private final boolean small;
  private String shortcutKey;
  private boolean busy;
  private String idleText;

  public DtaqButton(String text, Variant variant) {
    this(text, variant, false);
  }

  private DtaqButton(String text, Variant variant, boolean small) {
    super(text);
    this.variant = variant;
    this.small = small
        || variant == Variant.DANGER_SOFT
        || variant == Variant.SEGMENTED_ACTIVE
        || variant == Variant.SEGMENTED_INACTIVE;
    setFocusPainted(false);
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    applyVariant();
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
    return new DtaqButton(text, Variant.PRIMARY, true);
  }

  public static DtaqButton secondarySmall(String text) {
    return new DtaqButton(text, Variant.SECONDARY, true);
  }

  public static DtaqButton dangerSmall(String text) {
    return new DtaqButton(text, Variant.DANGER_SOFT, true);
  }

  public static DtaqButton link(String text) {
    return new DtaqButton(text, Variant.LINK);
  }

  public Variant getVariant() {
    return variant;
  }

  public void setVariant(Variant newVariant) {
    this.variant = newVariant;
    applyVariant();
    repaint();
  }

  public void setShortcutKey(String key) {
    this.shortcutKey = key;
    // Chừa lề phải cho nhãn phím tắt để chữ của nút không bị đè
    int reserved = (key == null || key.isEmpty())
        ? 0 : getFontMetrics(DtaqFonts.KBD).stringWidth(key) + KBD_PADDING * 2 + KBD_GAP;
    setMargin(new Insets(2, 14 + reserved, 2, 14 + reserved));
    revalidate();
    repaint();
  }

  /**
   * Bật/tắt trạng thái đang xử lý: vô hiệu hóa nút và hiện "Đang xử lý…"; tắt thì khôi phục nhãn cũ.
   */
  public void setBusy(boolean newBusy) {
    if (newBusy == busy) {
      return;
    }
    busy = newBusy;
    if (newBusy) {
      idleText = getText();
      setText(BUSY_TEXT);
      setEnabled(false);
    } else {
      setText(idleText);
      setEnabled(true);
    }
  }

  public boolean isBusy() {
    return busy;
  }

  private void applyVariant() {
    String extra = small ? DtaqStyles.SMALL_BUTTON_EXTRA : "";
    setFont(small ? DtaqFonts.SMALL_BUTTON : DtaqFonts.BUTTON);
    setBorderPainted(true);
    setContentAreaFilled(true);
    setOpaque(false);
    switch (variant) {
      case PRIMARY -> setStyle(DtaqStyles.BUTTON_PRIMARY + extra);
      case SECONDARY -> setStyle(DtaqStyles.BUTTON_SECONDARY + extra);
      case DANGER_SOFT -> setStyle(DtaqStyles.BUTTON_DANGER + extra);
      case SEGMENTED_ACTIVE -> setStyle(
          "background: " + DtaqStyles.hex(DtaqColors.PRIMARY_SOFT)
              + "; foreground: " + DtaqStyles.hex(DtaqColors.PRIMARY_STRONG)
              + "; borderWidth: 0; arc: 6" + extra);
      case SEGMENTED_INACTIVE -> setStyle(DtaqStyles.BUTTON_SECONDARY
          + "; foreground: " + DtaqStyles.hex(DtaqColors.TEXT_MUTED) + extra);
      case LINK -> {
        setFont(DtaqFonts.CAPTION);
        setForeground(DtaqColors.PRIMARY);
        setBorderPainted(false);
        setContentAreaFilled(false);
      }
    }
  }

  private void setStyle(String style) {
    putClientProperty(FlatClientProperties.STYLE, style);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    if (variant != Variant.LINK) {
      d.height = Math.max(d.height, small ? DtaqSpacing.BUTTON_H_SM : DtaqSpacing.BUTTON_H);
    }
    return d;
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    if (shortcutKey == null || shortcutKey.isEmpty()) {
      return;
    }
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setFont(DtaqFonts.KBD);
    Color keyColor;
    if (!isEnabled()) {
      keyColor = DtaqColors.TEXT_SUBTLE;
    } else {
      keyColor = variant == Variant.PRIMARY ? DtaqColors.PRIMARY_SOFT_2 : DtaqColors.TEXT_MUTED;
    }
    FontMetrics fm = g2.getFontMetrics();
    int textW = fm.stringWidth(shortcutKey);
    int boxW = textW + KBD_PADDING * 2;
    int boxH = fm.getHeight() + 2;
    int x = getWidth() - boxW - KBD_GAP;
    int y = (getHeight() - boxH) / 2;
    g2.setColor(new Color(keyColor.getRed(), keyColor.getGreen(), keyColor.getBlue(), 110));
    g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, boxW - 1, boxH - 1, 6, 6));
    g2.setColor(keyColor);
    g2.drawString(shortcutKey, x + KBD_PADDING, y + 1 + fm.getAscent());
    g2.dispose();
  }
}
