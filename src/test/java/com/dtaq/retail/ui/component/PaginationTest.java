package com.dtaq.retail.ui.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Kiểm tra logic thuần của thành phần giao diện")
class PaginationTest {

  @Test
  @DisplayName("Pagination.totalPages: luôn có tối thiểu 1 trang và làm tròn lên")
  void testTotalPages() {
    assertEquals(1, Pagination.totalPages(0, 10));
    assertEquals(1, Pagination.totalPages(10, 10));
    assertEquals(2, Pagination.totalPages(11, 10));
    assertEquals(3, Pagination.totalPages(23, 10));
    assertEquals(1, Pagination.totalPages(5, 0));
    assertEquals(1, Pagination.totalPages(-3, 10));
  }

  @Test
  @DisplayName("BarChartPanel.niceAxisMax: trục chia đều 4 khoảng ở bậc 1/2/2,5/5 × 10^n")
  void testNiceAxisMax() {
    assertEquals(4.0, BarChartPanel.niceAxisMax(0));
    assertEquals(80.0, BarChartPanel.niceAxisMax(72));
    assertEquals(100.0, BarChartPanel.niceAxisMax(85));
    assertEquals(40.0, BarChartPanel.niceAxisMax(35));
    assertEquals(8.0, BarChartPanel.niceAxisMax(7));
  }
}
