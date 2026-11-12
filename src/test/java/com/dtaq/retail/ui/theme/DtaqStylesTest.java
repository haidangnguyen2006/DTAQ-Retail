package com.dtaq.retail.ui.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Chuỗi style FlatLaf dựng từ design token")
class DtaqStylesTest {

  @Test
  @DisplayName("hex() trả về mã #RRGGBB viết hoa, không có kênh alpha")
  void testHex() {
    assertEquals("#006DD7", DtaqStyles.hex(DtaqColors.PRIMARY));
    assertEquals("#FFFFFF", DtaqStyles.hex(DtaqColors.SURFACE));
    assertEquals("#0384FF", DtaqStyles.hex(DtaqColors.FOCUS_RING));
  }

  @Test
  @DisplayName("Style nút chính dùng màu primary và bo góc chuẩn")
  void testPrimaryButtonStyle() {
    assertTrue(DtaqStyles.BUTTON_PRIMARY.contains("background: #006DD7"));
    assertTrue(DtaqStyles.BUTTON_PRIMARY.contains("hoverBackground: #0062C2"));
    assertTrue(DtaqStyles.BUTTON_PRIMARY.contains("arc: 12"));
  }
}
