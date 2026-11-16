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
  @DisplayName("Các mã màu Brand & Primary phải khớp chính xác đặc tả DTAQ Azure")
  void testBrandAndPrimaryColors() {
    assertEquals("#0384FF", toHex(DtaqColors.BRAND));
    assertEquals("#62A6FE", toHex(DtaqColors.BRAND_400));
    assertEquals("#006DD7", toHex(DtaqColors.PRIMARY));
    assertEquals("#0058B0", toHex(DtaqColors.PRIMARY_STRONG));
    assertEquals("#E4F0FF", toHex(DtaqColors.PRIMARY_SOFT));
    assertEquals("#C9E0FE", toHex(DtaqColors.PRIMARY_SOFT_2));
    assertEquals("#9DC7FF", toHex(DtaqColors.PRIMARY_BORDER_SOFT));
    assertEquals("#001D44", toHex(DtaqColors.BRAND_DARK));
    assertEquals("#88A7D0", toHex(DtaqColors.BRAND_ON_DARK_MUTED));
  }

  @Test
  @DisplayName("Các mã màu Neutral, Background và Border phải khớp đặc tả")
  void testNeutralColors() {
    assertEquals("#111B28", toHex(DtaqColors.TEXT));
    assertEquals("#5A6472", toHex(DtaqColors.TEXT_MUTED));
    assertEquals("#6B7583", toHex(DtaqColors.TEXT_SUBTLE));
    assertEquals("#F3F6FA", toHex(DtaqColors.BG_APP));
    assertEquals("#FFFFFF", toHex(DtaqColors.SURFACE));
    assertEquals("#F8FAFD", toHex(DtaqColors.SURFACE_SUNKEN));
    assertEquals("#DAE0E8", toHex(DtaqColors.BORDER));
    assertEquals("#AFB8C4", toHex(DtaqColors.BORDER_STRONG));
    assertEquals("#77818F", toHex(DtaqColors.BORDER_HOVER));
    assertEquals("#EAEEF4", toHex(DtaqColors.DISABLED_BG));
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
  @DisplayName("Các mã màu Biểu đồ phải khớp đặc tả (thứ tự cố định)")
  void testChartColors() {
    assertEquals("#1886FE", toHex(DtaqColors.CHART_1));
    assertEquals("#EB6834", toHex(DtaqColors.CHART_2));
    assertEquals("#1BAF7A", toHex(DtaqColors.CHART_3));
    assertEquals("#EDA100", toHex(DtaqColors.CHART_4));
    assertEquals("#6250D6", toHex(DtaqColors.CHART_5));
    assertEquals("#77818F", toHex(DtaqColors.CHART_6));
  }

  @Test
  @DisplayName("Sidebar dùng navy của logo")
  void testSidebarColors() {
    assertEquals("#001D44", toHex(DtaqColors.SIDEBAR_BG));
    assertEquals("#002957", toHex(DtaqColors.SIDEBAR_HOVER));
    assertEquals("#003671", toHex(DtaqColors.SIDEBAR_ACTIVE_BG));
    assertEquals("#C4D9F6", toHex(DtaqColors.SIDEBAR_TEXT));
    assertEquals("#62A6FE", toHex(DtaqColors.SIDEBAR_ACCENT));
  }

  @Test
  @DisplayName("Các token trạng thái và tương tác bổ sung phải đúng mã màu")
  void testStatusAndInteractionColors() {
    assertEquals("#0062C2", toHex(DtaqColors.PRIMARY_HOVER));
    assertEquals("#FAD7D4", toHex(DtaqColors.DANGER_SOFT_HOVER));
    assertEquals("#B4540A", toHex(DtaqColors.WARNING));
    assertEquals("#FFF4DB", toHex(DtaqColors.WARNING_SOFT));
    assertEquals("#F2F7FE", toHex(DtaqColors.ROW_HOVER));
    assertEquals("#0F7B55", toHex(DtaqColors.SUCCESS));
    assertEquals("#E4F5EC", toHex(DtaqColors.SUCCESS_SOFT));
  }
}
