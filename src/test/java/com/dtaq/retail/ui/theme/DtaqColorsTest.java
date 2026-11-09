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
    assertEquals("#2457F5", toHex(DtaqColors.PRIMARY));
    assertEquals("#1A3FB0", toHex(DtaqColors.PRIMARY_STRONG));
    assertEquals("#EEF3FF", toHex(DtaqColors.PRIMARY_SOFT));
    assertEquals("#DCE6FF", toHex(DtaqColors.PRIMARY_SOFT_2));
    assertEquals("#BCCDFF", toHex(DtaqColors.PRIMARY_BORDER_SOFT));
    assertEquals("#111826", toHex(DtaqColors.BRAND_DARK));
    assertEquals("#8B97AB", toHex(DtaqColors.BRAND_ON_DARK_MUTED));
  }

  @Test
  @DisplayName("Các mã màu Neutral, Background và Border phải khớp đặc tả")
  void testNeutralColors() {
    assertEquals("#151A24", toHex(DtaqColors.TEXT));
    assertEquals("#5B6577", toHex(DtaqColors.TEXT_MUTED));
    assertEquals("#8792A3", toHex(DtaqColors.TEXT_SUBTLE));
    assertEquals("#F4F6FA", toHex(DtaqColors.BG_APP));
    assertEquals("#FFFFFF", toHex(DtaqColors.SURFACE));
    assertEquals("#DDE2EB", toHex(DtaqColors.BORDER));
    assertEquals("#ECEFF5", toHex(DtaqColors.DISABLED_BG));
  }

  @Test
  @DisplayName("Các mã màu Cảnh báo và Danger phải khớp đặc tả")
  void testDangerColors() {
    assertEquals("#C2342C", toHex(DtaqColors.DANGER));
    assertEquals("#FDECEB", toHex(DtaqColors.DANGER_SOFT));
    assertEquals("#9F2620", toHex(DtaqColors.DANGER_STRONG));
    assertEquals("#FEF5F4", toHex(DtaqColors.DANGER_SOFT_2));
  }

  @Test
  @DisplayName("Các mã màu Biểu đồ Top 5 phải khớp đặc tả")
  void testChartColors() {
    assertEquals("#2457F5", toHex(DtaqColors.CHART_1));
    assertEquals("#12A594", toHex(DtaqColors.CHART_2));
    assertEquals("#D98A0B", toHex(DtaqColors.CHART_3));
    assertEquals("#8E5CF6", toHex(DtaqColors.CHART_4));
    assertEquals("#E5484D", toHex(DtaqColors.CHART_5));
  }

  @Test
  @DisplayName("Các token trạng thái và tương tác bổ sung phải đúng mã màu")
  void testStatusAndInteractionColors() {
    assertEquals("#1F4FD8", toHex(DtaqColors.PRIMARY_HOVER));
    assertEquals("#FAD7D4", toHex(DtaqColors.DANGER_SOFT_HOVER));
    assertEquals("#B4540A", toHex(DtaqColors.WARNING));
    assertEquals("#FFF4DB", toHex(DtaqColors.WARNING_SOFT));
    assertEquals("#F7F9FC", toHex(DtaqColors.ROW_HOVER));
    assertEquals("#0F7B55", toHex(DtaqColors.SUCCESS));
    assertEquals("#E4F5EC", toHex(DtaqColors.SUCCESS_SOFT));
  }
}
