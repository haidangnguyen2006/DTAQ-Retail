package com.dtaq.retail.ui.theme;

/**
 * Khoảng cách, kích thước, bo góc chuẩn (design tokens) theo thiết kế Figma DTAQ.
 */
public final class DtaqSpacing {

  private DtaqSpacing() {
  }

  // --- Corner Radii ---
  public static final int RADIUS_S = 6;
  public static final int RADIUS_M = 8;
  public static final int RADIUS_TABLE = 10;
  public static final int RADIUS_CARD_SM = 10;
  public static final int RADIUS_L = 12;
  public static final int RADIUS_DIALOG = 16;
  public static final int RADIUS_PILL = 100;

  // --- Heights & Dimensions ---
  public static final int CONTROL_H = 36;
  public static final int BUTTON_H = 36;
  public static final int BUTTON_H_SM = 28;
  public static final int PILL_H = 22;
  public static final int TOPBAR_H = 56;
  public static final int SIDEBAR_W = 248;
  public static final int NAV_ITEM_H = 38;
  public static final int TABLE_HEADER_H = 36;
  public static final int TABLE_ROW_H = 40;

  // --- Spacings & Paddings ---
  public static final int PAGE_PADDING = 24;
  public static final int SECTION_GAP = 20;
  public static final int CARD_PADDING = 20;
  public static final int CARD_PADDING_COMPACT = 16;
  public static final int CARD_PADDING_KPI = 18;
  public static final int CARD_GAP = 16;
  public static final int CARD_GAP_COMPACT = 12;
  public static final int FIELD_LABEL_GAP = 6;
  public static final int FORM_FIELD_GAP = 16;
  public static final int FORM_FIELD_GAP_LOGIN = 20;
  public static final int KPI_GAP = 16;
  public static final int KPI_GAP_COMPACT = 12;

  // --- Bổ sung ---
  public static final int SIDEBAR_RAIL_W = 68;
  public static final int CONTROL_H_LG = 44;
  public static final int TABLE_ROW_H_COMPACT = 34;
  public static final int TABLE_ROW_H_TWO_LINE = 52;
  public static final int CONTENT_MAX_W = 1680;
  public static final int DIALOG_MARGIN = 24;

  /** Đường kính cung bo của FlatLaf ({@code arc}) ứng với bán kính {@code radius}. */
  public static int arc(int radius) {
    return radius * 2;
  }

  /** Chuẩn hóa chiều cao hàng bảng mà trang yêu cầu về 3 mức chuẩn của giao diện. */
  public static int normalizeRowHeight(int requested) {
    if (requested <= 38) {
      return TABLE_ROW_H_COMPACT;
    }
    return requested <= 50 ? TABLE_ROW_H : TABLE_ROW_H_TWO_LINE;
  }
}
