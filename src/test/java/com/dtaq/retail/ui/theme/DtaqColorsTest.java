package com.dtaq.retail.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Kiểm tra mã màu Design Tokens DTAQ")
class DtaqColorsTest {

  private static String toHex(Color c) {
    return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
  }

  @Test
  @DisplayName("Các mã màu Brand & Primary phải khớp chính xác đặc tả Figma")
  void testBrandAndPrimaryColors() {
    assertEquals("#087F75", toHex(DtaqColors.PRIMARY));
    assertEquals("#075E57", toHex(DtaqColors.PRIMARY_STRONG));
    assertEquals("#E8F5F2", toHex(DtaqColors.PRIMARY_SOFT));
    assertEquals("#D6EEEA", toHex(DtaqColors.PRIMARY_SOFT_2));
    assertEquals("#B5DCD6", toHex(DtaqColors.PRIMARY_BORDER_SOFT));
    assertEquals("#163E43", toHex(DtaqColors.BRAND_DARK));
    assertEquals("#BDDBD6", toHex(DtaqColors.BRAND_ON_DARK_MUTED));
  }

  @Test
  @DisplayName("Các mã màu Neutral, Background và Border phải khớp đặc tả")
  void testNeutralColors() {
    assertEquals("#18252D", toHex(DtaqColors.TEXT));
    assertEquals("#667580", toHex(DtaqColors.TEXT_MUTED));
    assertEquals("#93A0AA", toHex(DtaqColors.TEXT_SUBTLE));
    assertEquals("#F4F6F8", toHex(DtaqColors.BG_APP));
    assertEquals("#FFFFFF", toHex(DtaqColors.SURFACE));
    assertEquals("#E2E8EC", toHex(DtaqColors.BORDER));
    assertEquals("#E5EBEF", toHex(DtaqColors.DISABLED_BG));
  }

  @Test
  @DisplayName("Các mã màu Cảnh báo và Danger phải khớp đặc tả")
  void testDangerColors() {
    assertEquals("#B42318", toHex(DtaqColors.DANGER));
    assertEquals("#FEE4E2", toHex(DtaqColors.DANGER_SOFT));
    assertEquals("#7F0808", toHex(DtaqColors.DANGER_STRONG));
    assertEquals("#F5E8E8", toHex(DtaqColors.DANGER_SOFT_2));
  }

  @Test
  @DisplayName("Các mã màu Biểu đồ Top 5 phải khớp đặc tả")
  void testChartColors() {
    assertEquals("#087F75", toHex(DtaqColors.CHART_1));
    assertEquals("#439D93", toHex(DtaqColors.CHART_2));
    assertEquals("#79B8AE", toHex(DtaqColors.CHART_3));
    assertEquals("#ABD1C9", toHex(DtaqColors.CHART_4));
    assertEquals("#D4E7E2", toHex(DtaqColors.CHART_5));
  }
}
