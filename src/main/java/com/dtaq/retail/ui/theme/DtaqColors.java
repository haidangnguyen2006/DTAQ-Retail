package com.dtaq.retail.ui.theme;

import java.awt.Color;

/**
 * Bảng màu chuẩn (design tokens) của giao diện "Volt Console".
 * Nguồn duy nhất cho màu sắc trong ứng dụng; các tên hằng giữ ổn định, chỉ giá trị thay đổi.
 */
public final class DtaqColors {

  private DtaqColors() {
  }

  /** Trả màu {@code c} với độ trong suốt {@code a} (0..1). */
  public static Color alpha(Color c, float a) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * a));
  }

  // --- Brand & Primary (Volt) ---
  public static final Color PRIMARY = new Color(0x2457F5);
  public static final Color PRIMARY_STRONG = new Color(0x1A3FB0);
  public static final Color PRIMARY_HOVER = new Color(0x1F4FD8);
  public static final Color PRIMARY_SOFT = new Color(0xEEF3FF);
  public static final Color PRIMARY_SOFT_2 = new Color(0xDCE6FF);
  public static final Color PRIMARY_BORDER_SOFT = new Color(0xBCCDFF);

  public static final Color BRAND_DARK = new Color(0x111826);
  public static final Color BRAND_ON_DARK_MUTED = new Color(0x8B97AB);

  // --- Neutral / Text / Surface (Ink) ---
  public static final Color TEXT = new Color(0x151A24);
  public static final Color TEXT_MUTED = new Color(0x5B6577);
  public static final Color TEXT_SUBTLE = new Color(0x8792A3);

  public static final Color BG_APP = new Color(0xF4F6FA);
  public static final Color SURFACE = new Color(0xFFFFFF);
  public static final Color SURFACE_SUNKEN = new Color(0xFAFBFD);
  public static final Color BORDER = new Color(0xDDE2EB);
  public static final Color BORDER_STRONG = new Color(0xC5CDD9);
  public static final Color BORDER_HOVER = new Color(0x8792A3);
  public static final Color DISABLED_BG = new Color(0xECEFF5);

  // --- Danger ---
  public static final Color DANGER = new Color(0xC2342C);
  public static final Color DANGER_SOFT = new Color(0xFDECEB);
  public static final Color DANGER_STRONG = new Color(0x9F2620);
  public static final Color DANGER_SOFT_2 = new Color(0xFEF5F4);
  public static final Color DANGER_SOFT_HOVER = new Color(0xFAD7D4);

  // --- Status ---
  public static final Color SUCCESS = new Color(0x0F7B55);
  public static final Color SUCCESS_SOFT = new Color(0xE4F5EC);
  public static final Color WARNING = new Color(0xB4540A);
  public static final Color WARNING_SOFT = new Color(0xFFF4DB);
  public static final Color INFO = PRIMARY;
  public static final Color INFO_SOFT = PRIMARY_SOFT;

  // --- Accent (tiền nổi bật / khuyến mãi) ---
  public static final Color ACCENT = new Color(0xF5A524);
  public static final Color ACCENT_SOFT = new Color(0xFFF7E6);
  public static final Color ACCENT_TEXT = new Color(0x7A4A00);

  // --- Interaction ---
  public static final Color ROW_HOVER = new Color(0xF7F9FC);
  public static final Color FOCUS_RING = alpha(PRIMARY, 0.40f);

  // --- Overlay & Shadow ---
  public static final Color OVERLAY = alpha(new Color(0x0E121A), 0.45f);
  public static final Color SHADOW = alpha(new Color(0x0E121A), 0.12f);

  // --- Sidebar (nền tối) ---
  public static final Color SIDEBAR_BG = new Color(0x111826);
  public static final Color SIDEBAR_HOVER = new Color(0x1A2233);
  public static final Color SIDEBAR_ACTIVE_BG = new Color(0x1E2A47);
  public static final Color SIDEBAR_TEXT = new Color(0xC9D1DD);
  public static final Color SIDEBAR_TEXT_MUTED = new Color(0x8B97AB);
  public static final Color SIDEBAR_ACTIVE_TEXT = new Color(0xFFFFFF);
  public static final Color SIDEBAR_ACCENT = new Color(0x8EA9FC);
  public static final Color SIDEBAR_DIVIDER = new Color(0x1F2633);

  // --- Charts ---
  public static final Color CHART_1 = new Color(0x2457F5);
  public static final Color CHART_2 = new Color(0x12A594);
  public static final Color CHART_3 = new Color(0xD98A0B);
  public static final Color CHART_4 = new Color(0x8E5CF6);
  public static final Color CHART_5 = new Color(0xE5484D);
  public static final Color CHART_6 = new Color(0x64748B);
  public static final Color CHART_GRID = new Color(0xECEFF5);
  public static final Color CHART_AXIS = new Color(0x5B6577);

  // --- Link Accent ---
  public static final Color LINK_ACCENT = PRIMARY;
}
