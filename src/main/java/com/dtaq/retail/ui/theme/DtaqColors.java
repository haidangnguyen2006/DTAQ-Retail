package com.dtaq.retail.ui.theme;

import java.awt.Color;

/**
 * Bảng màu chuẩn (design tokens) theo thiết kế Figma DTAQ.
 * Nguồn duy nhất cho màu sắc trong ứng dụng.
 */
public final class DtaqColors {

  private DtaqColors() {
  }

  // --- Brand & Primary ---
  public static final Color PRIMARY = new Color(0x087F75);
  public static final Color PRIMARY_STRONG = new Color(0x075E57);
  public static final Color PRIMARY_SOFT = new Color(0xE8F5F2);
  public static final Color PRIMARY_SOFT_2 = new Color(0xD6EEEA);
  public static final Color PRIMARY_BORDER_SOFT = new Color(0xB5DCD6);

  public static final Color BRAND_DARK = new Color(0x163E43);
  public static final Color BRAND_ON_DARK_MUTED = new Color(0xBDDBD6);

  // --- Neutral / Text / Surface ---
  public static final Color TEXT = new Color(0x18252D);
  public static final Color TEXT_MUTED = new Color(0x667580);
  public static final Color TEXT_SUBTLE = new Color(0x93A0AA);

  public static final Color BG_APP = new Color(0xF4F6F8);
  public static final Color SURFACE = new Color(0xFFFFFF);
  public static final Color BORDER = new Color(0xE2E8EC);
  public static final Color DISABLED_BG = new Color(0xE5EBEF);

  // --- Danger & Alerts ---
  public static final Color DANGER = new Color(0xB42318);
  public static final Color DANGER_SOFT = new Color(0xFEE4E2);
  public static final Color DANGER_STRONG = new Color(0x7F0808);
  public static final Color DANGER_SOFT_2 = new Color(0xF5E8E8);

  // --- Overlay & Shadow ---
  public static final Color OVERLAY = new Color(0x15, 0x2A, 0x3D, (int) (255 * 0.40f));
  public static final Color SHADOW = new Color(0x10, 0x26, 0x35, (int) (255 * 0.15f));

  // --- Charts (Top 5 Dòng sản phẩm) ---
  public static final Color CHART_1 = new Color(0x087F75);
  public static final Color CHART_2 = new Color(0x439D93);
  public static final Color CHART_3 = new Color(0x79B8AE);
  public static final Color CHART_4 = new Color(0xABD1C9);
  public static final Color CHART_5 = new Color(0xD4E7E2);

  // --- Link Accent (quy về primary chuẩn) ---
  public static final Color LINK_ACCENT = PRIMARY;
}
