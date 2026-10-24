package com.dtaq.retail.ui.page.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Ngưỡng số ngày tồn kho do Quản lý nhập (UC014)")
class LongStockThresholdTest {

  @Test
  @DisplayName("Chỉ chấp nhận số nguyên dương; rỗng hoặc sai định dạng trả về 0")
  void testParseThreshold() {
    assertEquals(60, LongStockReportPage.parseThreshold("60"));
    assertEquals(90, LongStockReportPage.parseThreshold("  90 "));
    assertEquals(0, LongStockReportPage.parseThreshold(""));
    assertEquals(0, LongStockReportPage.parseThreshold(null));
    assertEquals(0, LongStockReportPage.parseThreshold("-5"));
    assertEquals(0, LongStockReportPage.parseThreshold("6.5"));
    assertEquals(0, LongStockReportPage.parseThreshold("abc"));
    assertEquals(0, LongStockReportPage.parseThreshold("0"));
  }
}
