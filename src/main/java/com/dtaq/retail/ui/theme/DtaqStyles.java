package com.dtaq.retail.ui.theme;

import java.awt.Color;

/**
 * Chuỗi style FlatLaf ({@code FlatClientProperties.STYLE}) dựng từ {@link DtaqColors}.
 * Nguồn duy nhất cho style của control; component không tự viết mã màu hex.
 */
public final class DtaqStyles {

  private DtaqStyles() {
  }

  /** Mã màu dạng {@code #RRGGBB} dùng trong chuỗi style FlatLaf. */
  public static String hex(Color c) {
    return String.format("#%06X", c.getRGB() & 0xFFFFFF);
  }

  public static final String TEXT_FIELD =
      "arc: 12; borderColor: " + hex(DtaqColors.BORDER)
          + "; focusedBorderColor: " + hex(DtaqColors.PRIMARY);

  public static final String BUTTON_PRIMARY =
      "background: " + hex(DtaqColors.PRIMARY)
          + "; foreground: " + hex(DtaqColors.SURFACE)
          + "; hoverBackground: " + hex(DtaqColors.PRIMARY_HOVER)
          + "; pressedBackground: " + hex(DtaqColors.PRIMARY_STRONG)
          + "; disabledBackground: " + hex(DtaqColors.DISABLED_BG)
          + "; disabledText: " + hex(DtaqColors.TEXT_SUBTLE)
          + "; borderWidth: 0; arc: 12";

  public static final String BUTTON_SECONDARY =
      "background: " + hex(DtaqColors.SURFACE)
          + "; foreground: " + hex(DtaqColors.TEXT)
          + "; borderColor: " + hex(DtaqColors.BORDER)
          + "; hoverBackground: " + hex(DtaqColors.BG_APP)
          + "; pressedBackground: " + hex(DtaqColors.DISABLED_BG)
          + "; disabledBackground: " + hex(DtaqColors.SURFACE)
          + "; disabledText: " + hex(DtaqColors.TEXT_SUBTLE)
          + "; disabledBorderColor: " + hex(DtaqColors.BORDER)
          + "; borderWidth: 1; arc: 12";

  public static final String BUTTON_DANGER =
      "background: " + hex(DtaqColors.DANGER_SOFT)
          + "; foreground: " + hex(DtaqColors.DANGER)
          + "; hoverBackground: " + hex(DtaqColors.DANGER_SOFT_HOVER)
          + "; pressedBackground: " + hex(DtaqColors.DANGER_SOFT_HOVER)
          + "; disabledBackground: " + hex(DtaqColors.DISABLED_BG)
          + "; disabledText: " + hex(DtaqColors.TEXT_SUBTLE)
          + "; borderWidth: 0; arc: 12";

  /** Nút nhỏ trong ô bảng / thanh công cụ: thêm lề ngang gọn và bỏ chiều rộng tối thiểu. */
  public static final String SMALL_BUTTON_EXTRA = "; minimumWidth: 0; margin: 2,12,2,12";

  public static final String TOGGLE_SEGMENT =
      "background: " + hex(DtaqColors.SURFACE)
          + "; foreground: " + hex(DtaqColors.TEXT_MUTED)
          + "; borderColor: " + hex(DtaqColors.BORDER)
          + "; hoverBackground: " + hex(DtaqColors.BG_APP)
          + "; selectedBackground: " + hex(DtaqColors.PRIMARY_SOFT)
          + "; selectedForeground: " + hex(DtaqColors.PRIMARY_STRONG)
          + "; borderWidth: 1; arc: 12; minimumWidth: 0; margin: 2,14,2,14";

  public static String navItem(boolean active) {
    return "background: " + hex(active ? DtaqColors.PRIMARY_SOFT : DtaqColors.SURFACE)
        + "; foreground: " + hex(active ? DtaqColors.PRIMARY_STRONG : DtaqColors.TEXT_MUTED)
        + "; hoverBackground: " + hex(active ? DtaqColors.PRIMARY_SOFT : DtaqColors.BG_APP)
        + "; pressedBackground: " + hex(DtaqColors.PRIMARY_SOFT_2)
        + "; borderWidth: 0; arc: 12; minimumWidth: 0; margin: 0,14,0,12";
  }
}
