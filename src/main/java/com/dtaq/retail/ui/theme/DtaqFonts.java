package com.dtaq.retail.ui.theme;

import com.formdev.flatlaf.fonts.inter.FlatInterFont;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;
import javax.swing.UIManager;

/**
 * Typography tokens chuẩn theo thiết kế Figma DTAQ.
 * Sử dụng font Inter với các biến thể Regular, Medium, Semi Bold, Bold, Italic.
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
    Font base = font(style, size);
    if (Math.abs(trackingPx) < 0.001f) {
      return base;
    }
    float trackingValue = trackingPx / size;
    return base.deriveFont(Map.of(TextAttribute.TRACKING, trackingValue));
  }

  // --- Display & Headings ---
  public static final Font DISPLAY = font(Font.BOLD, 32f, -1.0f);
  public static final Font AMOUNT_XL = font(Font.PLAIN, 32f, -1.0f);
  public static final Font PAGE_TITLE = font(Font.PLAIN, 28f, -0.7f);
  public static final Font KPI_VALUE = font(Font.PLAIN, 29f, -0.8f);
  public static final Font DIALOG_TITLE = font(Font.PLAIN, 24f, -0.6f);
  public static final Font DONUT_CENTER = font(Font.BOLD, 24f);
  public static final Font BRAND = font(Font.PLAIN, 23f, 1.0f);

  // --- Section & Card Titles ---
  public static final Font SECTION_TITLE_STRONG = font(Font.BOLD, 18f);
  public static final Font CUSTOMER_NAME = font(Font.BOLD, 18f);
  public static final Font CARD_TITLE = font(Font.PLAIN, 16f);
  public static final Font CARD_TITLE_SEMI = font(Font.BOLD, 16f);
  public static final Font EMPTY_TITLE = font(Font.BOLD, 15f);

  // --- Body & Controls (14px) ---
  public static final Font BODY_14 = font(Font.PLAIN, 14f);
  public static final Font BUTTON = font(Font.BOLD, 14f);
  public static final Font AMOUNT_ROW = font(Font.BOLD, 14f);

  // --- Body, Table & Nav (13px) ---
  public static final Font BODY_13 = font(Font.PLAIN, 13f);
  public static final Font BODY_13_MEDIUM = font(Font.BOLD, 13f);
  public static final Font LABEL = font(Font.BOLD, 13f);

  // --- Table Header & Pills & Small Buttons (12px) ---
  public static final Font TABLE_HEADER = font(Font.BOLD, 12f);
  public static final Font CAPTION = font(Font.PLAIN, 12f);
  public static final Font PILL = font(Font.BOLD, 12f);
  public static final Font SMALL_BUTTON = font(Font.BOLD, 12f);
  public static final Font EYEBROW = font(Font.BOLD, 12f, 1.2f);
  public static final Font ITALIC_HINT = font(Font.ITALIC, 12f);

  // --- Hints & Micro (10-11px) ---
  public static final Font HINT = font(Font.PLAIN, 11f);
  public static final Font HINT_ON_PRIMARY = font(Font.BOLD, 11f);
  public static final Font OVERLINE = font(Font.BOLD, 10f, 1.3f);
  public static final Font MICRO = font(Font.PLAIN, 10f);
}
