package com.dtaq.retail.ui.theme;

import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;
import javax.swing.UIManager;

/**
 * Typography tokens chuẩn theo thiết kế Figma DTAQ.
 * Sử dụng font Inter: Regular cho nội dung, SemiBold cho nhấn mạnh/tiêu đề (không dùng Bold nặng).
 */
public final class DtaqFonts {

  private DtaqFonts() {
  }

  public static String fontFamily() {
    Font defaultFont = UIManager.getFont("defaultFont");
    return (defaultFont != null) ? defaultFont.getFamily() : FlatInterFont.FAMILY;
  }

  public static Font font(int style, float size) {
    return new Font(fontFamily(), style, Math.round(size)).deriveFont(size);
  }

  public static Font font(int style, float size, float trackingPx) {
    return withTracking(font(style, size), size, trackingPx);
  }

  /** Inter SemiBold; tự rơi về chữ đậm của font mặc định nếu họ SemiBold không khả dụng. */
  public static Font semibold(float size) {
    return semibold(size, 0f);
  }

  public static Font semibold(float size, float trackingPx) {
    Font f = new Font(FlatInterFont.FAMILY_SEMIBOLD, Font.PLAIN, Math.round(size)).deriveFont(size);
    if (!FlatInterFont.FAMILY_SEMIBOLD.equalsIgnoreCase(f.getFamily())) {
      return font(Font.BOLD, size, trackingPx);
    }
    return withTracking(f, size, trackingPx);
  }

  private static Font withTracking(Font base, float size, float trackingPx) {
    if (Math.abs(trackingPx) < 0.001f) {
      return base;
    }
    return base.deriveFont(Map.of(TextAttribute.TRACKING, trackingPx / size));
  }

  // --- Display & Headings ---
  public static final Font DISPLAY = semibold(28f, -0.6f);
  public static final Font AMOUNT_XL = semibold(32f, -0.8f);
  public static final Font PAGE_TITLE = semibold(22f, -0.4f);
  public static final Font KPI_VALUE = semibold(26f, -0.5f);
  public static final Font DIALOG_TITLE = semibold(18f, -0.2f);
  public static final Font DONUT_CENTER = semibold(20f, -0.3f);
  public static final Font BRAND = semibold(17f, 0.4f);

  // --- Section & Card Titles ---
  public static final Font SECTION_TITLE_STRONG = semibold(15f);
  public static final Font CUSTOMER_NAME = semibold(16f);
  public static final Font CARD_TITLE = semibold(15f);
  public static final Font CARD_TITLE_SEMI = semibold(15f);
  public static final Font EMPTY_TITLE = semibold(14f);

  // --- Body & Controls (14px) ---
  public static final Font BODY_14 = font(Font.PLAIN, 14f);
  public static final Font BUTTON = semibold(13f);
  public static final Font AMOUNT_ROW = semibold(14f);

  // --- Body, Table & Nav (13px) ---
  public static final Font BODY_13 = font(Font.PLAIN, 13f);
  public static final Font BODY_13_MEDIUM = semibold(13f);
  public static final Font LABEL = semibold(13f);

  // --- Table Header & Pills & Small Buttons (12px) ---
  public static final Font TABLE_HEADER = semibold(12f, 0.1f);
  public static final Font CAPTION = font(Font.PLAIN, 12f);
  public static final Font PILL = semibold(12f);
  public static final Font SMALL_BUTTON = semibold(12f);
  public static final Font EYEBROW = semibold(11f, 0.6f);
  public static final Font ITALIC_HINT = font(Font.ITALIC, 12f);

  // --- Hints & Micro (10-11px) ---
  public static final Font HINT = font(Font.PLAIN, 11f);
  public static final Font HINT_ON_PRIMARY = semibold(11f);
  public static final Font KBD = semibold(11f);
  public static final Font OVERLINE = semibold(11f, 0.8f);
  public static final Font MICRO = font(Font.PLAIN, 10.5f);

  // --- Bổ sung ---
  public static final Font MONEY_TABLE = semibold(13f);
  public static final Font CODE = semibold(12.5f, 0.2f);
  public static final Font MONEY_INPUT = semibold(18f);
}
