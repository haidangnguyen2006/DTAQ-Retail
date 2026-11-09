package com.dtaq.retail.ui.page.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dtaq.retail.util.DateRange;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SalesByProductPageTest {

  @Test
  void recommendationBadge_TheoDoiVaSoLuongBan() {
    DateRange range = new DateRange(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 30));

    assertEquals(
        "🟢 Cần nhập thêm tháng này",
        SalesByProductPage.recommendationBadge(12, 120, 5, BigDecimal.valueOf(10_000_000), range));

    assertEquals(
        "🔵 Đủ hàng / Bỏ qua",
        SalesByProductPage.recommendationBadge(90, 90, 5, BigDecimal.valueOf(20_000_000), range));

    assertEquals(
        "🟡 Tồn kho cao / Giãn đơn",
        SalesByProductPage.recommendationBadge(150, 90, 5, BigDecimal.valueOf(25_000_000), range));

    assertEquals(
        "🔴 Xả kho / Ngừng nhập",
        SalesByProductPage.recommendationBadge(200, 0, 0, BigDecimal.ZERO, range));
  }
}
