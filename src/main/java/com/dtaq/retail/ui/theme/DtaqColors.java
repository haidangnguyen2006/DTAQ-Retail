package com.dtaq.retail.ui.theme;

import java.awt.Color;

/**
 * Bảng màu chuẩn (design tokens) của giao diện DTAQ Azure — màu lấy từ logo
 * (azure {@code #0384FF}, navy {@code #00183C}), thang dựng trong OKLCH và đã kiểm tương phản WCAG.
 * Nguồn duy nhất cho màu sắc trong ứng dụng; các tên hằng giữ ổn định, chỉ giá trị thay đổi.
 */
public final class DtaqColors {

  private DtaqColors() {
  }

  /** Trả màu {@code c} với độ trong suốt {@code a} (0..1). */
  public static Color alpha(Color c, float a) {
    return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(255 * a));
  }

  // --- Thương hiệu (màu gốc của logo; chỉ dùng cho hình, không dùng cho chữ nhỏ/nền nút) ---
  public static final Color BRAND = new Color(0x0384FF);
  public static final Color BRAND_400 = new Color(0x62A6FE);

  // --- Primary (hành động) — #006DD7 thay vì màu logo vì chữ trắng cần >= 4,5:1 ---
  public static final Color PRIMARY = new Color(0x006DD7);
  public static final Color PRIMARY_STRONG = new Color(0x0058B0);
  public static final Color PRIMARY_HOVER = new Color(0x0062C2);
  public static final Color PRIMARY_SOFT = new Color(0xE4F0FF);
  public static final Color PRIMARY_SOFT_2 = new Color(0xC9E0FE);
  public static final Color PRIMARY_BORDER_SOFT = new Color(0x9DC7FF);

  public static final Color BRAND_DARK = new Color(0x001D44);
  public static final Color BRAND_ON_DARK_MUTED = new Color(0x88A7D0);

  // --- Neutral / Text / Surface ---
  public static final Color TEXT = new Color(0x111B28);
  public static final Color TEXT_MUTED = new Color(0x5A6472);
  public static final Color TEXT_SUBTLE = new Color(0x6B7583);

  public static final Color BG_APP = new Color(0xF3F6FA);
  public static final Color SURFACE = new Color(0xFFFFFF);
  public static final Color SURFACE_SUNKEN = new Color(0xF8FAFD);
  public static final Color BORDER = new Color(0xDAE0E8);
  public static final Color BORDER_STRONG = new Color(0xAFB8C4);
  public static final Color BORDER_HOVER = new Color(0x77818F);
  public static final Color DISABLED_BG = new Color(0xEAEEF4);

  // --- Danger ---
  public static final Color DANGER = new Color(0xC2342C);
  public static final Color DANGER_SOFT = new Color(0xFDECEB);
  public static final Color DANGER_STRONG = new Color(0x9F2620);
  public static final Color DANGER_SOFT_2 = new Color(0xFEF5F4);
  public static final Color DANGER_SOFT_HOVER = new Color(0xFAD7D4);
  public static final Color DANGER_BORDER = new Color(0xF2B8B3);

  // --- Status ---
  public static final Color SUCCESS = new Color(0x0F7B55);
  public static final Color SUCCESS_SOFT = new Color(0xE4F5EC);
  public static final Color WARNING = new Color(0xB4540A);
  public static final Color WARNING_SOFT = new Color(0xFFF4DB);
  public static final Color WARNING_BORDER = new Color(0xF0D9A0);
  public static final Color INFO = PRIMARY;
  public static final Color INFO_SOFT = PRIMARY_SOFT;

  // --- Accent (tiền nổi bật / khuyến mãi) — hổ phách đi cùng xanh ---
  public static final Color ACCENT = new Color(0xF5A524);
  public static final Color ACCENT_SOFT = new Color(0xFFF7E6);
  public static final Color ACCENT_TEXT = new Color(0x7A4A00);

  // --- Interaction ---
  public static final Color ROW_HOVER = new Color(0xF2F7FE);
  public static final Color FOCUS_RING = alpha(BRAND, 0.45f);

  // --- Overlay & Shadow ---
  public static final Color OVERLAY = alpha(new Color(0x07101C), 0.50f);
  /** Nền phủ đặc khi máy không hỗ trợ cửa sổ trong suốt. */
  public static final Color OVERLAY_OPAQUE = new Color(0xC7CED8);
  public static final Color SHADOW = alpha(new Color(0x07101C), 0.12f);

  // --- Pill trung tính, Toast ---
  public static final Color PILL_NEUTRAL_TEXT = new Color(0x3E4957);
  public static final Color TOAST_BG = new Color(0x111B28);
  public static final Color TOAST_SUCCESS = new Color(0x5DD39E);
  public static final Color TOAST_ERROR = new Color(0xFF8A85);

  // --- Sidebar (nền navy của logo) ---
  public static final Color SIDEBAR_BG = new Color(0x001D44);
  public static final Color SIDEBAR_HOVER = new Color(0x002957);
  public static final Color SIDEBAR_ACTIVE_BG = new Color(0x003671);
  public static final Color SIDEBAR_TEXT = new Color(0xC4D9F6);
  public static final Color SIDEBAR_TEXT_MUTED = new Color(0x88A7D0);
  public static final Color SIDEBAR_ACTIVE_TEXT = new Color(0xFFFFFF);
  public static final Color SIDEBAR_ACCENT = new Color(0x62A6FE);
  public static final Color SIDEBAR_DIVIDER = new Color(0x112E52);
  public static final Color SIDEBAR_SCROLL_THUMB = new Color(0x1E4273);
  public static final Color SIDEBAR_SCROLL_THUMB_HOVER = new Color(0x2F5C99);

  // --- Charts (thứ tự cố định; đã kiểm phân biệt cho người mù màu) ---
  public static final Color CHART_1 = new Color(0x1886FE);
  public static final Color CHART_2 = new Color(0xEB6834);
  public static final Color CHART_3 = new Color(0x1BAF7A);
  public static final Color CHART_4 = new Color(0xEDA100);
  public static final Color CHART_5 = new Color(0x6250D6);
  /** Chỉ dành cho nhóm "Khác". */
  public static final Color CHART_6 = new Color(0x77818F);
  public static final Color CHART_GRID = new Color(0xEAEEF4);
  public static final Color CHART_AXIS = new Color(0x5A6472);

  // --- Link Accent ---
  public static final Color LINK_ACCENT = PRIMARY;
}
