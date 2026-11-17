package com.dtaq.retail.ui.page.invoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử thuật toán gợi ý tiền khách đưa của màn hình lập hóa đơn. */
class CashSuggestionsTest {

  @Test
  @DisplayName("Gợi ý cho hóa đơn 35.000 phải bỏ qua mốc 36.000 và dừng ở 6 lựa chọn")
  void goiYCho35Nghin() {
    assertEquals(
        List.of(35_000L, 40_000L, 50_000L, 100_000L, 200_000L, 500_000L),
        CashSuggestions.forTotal(35_000));
  }

  @Test
  @DisplayName("Gợi ý cho hóa đơn 87.000 bỏ mốc 88.000 vì khách có thể đưa 87.000")
  void goiYCho87Nghin() {
    assertEquals(
        List.of(87_000L, 90_000L, 100_000L, 200_000L, 500_000L),
        CashSuggestions.forTotal(87_000));
  }

  @Test
  @DisplayName("Gợi ý cho hóa đơn 235.000 gồm các mốc tròn từ 235.000 đến 500.000")
  void goiYCho235Nghin() {
    assertEquals(
        List.of(235_000L, 240_000L, 250_000L, 300_000L, 400_000L, 500_000L),
        CashSuggestions.forTotal(235_000));
  }

  @Test
  @DisplayName("Gợi ý cho hóa đơn 460.000 chỉ còn 460.000 và 500.000")
  void goiYCho460Nghin() {
    assertEquals(List.of(460_000L, 500_000L), CashSuggestions.forTotal(460_000));
  }

  @Test
  @DisplayName("Tổng thanh toán không dương không có gợi ý nào")
  void tongKhongDuongKhongGoiY() {
    assertEquals(List.of(), CashSuggestions.forTotal(0));
    assertEquals(List.of(), CashSuggestions.forTotal(-5));
  }

  @Test
  @DisplayName("Phân rã 36.000 và 460.000 theo mệnh giá lớn trước")
  void phanRaTheoMenhGia() {
    assertEquals(List.of(20_000L, 10_000L, 5_000L, 1_000L), CashSuggestions.breakdown(36_000));
    assertEquals(
        List.of(200_000L, 200_000L, 50_000L, 10_000L), CashSuggestions.breakdown(460_000));
  }

  @Test
  @DisplayName("Phân rã số tiền không trả được bằng mệnh giá hiện có là danh sách rỗng")
  void phanRaKhongHopLe() {
    assertEquals(List.of(), CashSuggestions.breakdown(1_500));
    assertEquals(List.of(), CashSuggestions.breakdown(0));
    assertEquals(List.of(), CashSuggestions.breakdown(-1_000));
  }

  @Test
  @DisplayName("Mô tả cách phân rã gộp các mệnh giá lặp lại")
  void moTaCachPhanRa() {
    assertEquals("20.000 + 10.000 + 5.000 + 1.000", CashSuggestions.describe(36_000));
    assertEquals("200.000 × 2 + 50.000 + 10.000", CashSuggestions.describe(460_000));
    assertEquals("", CashSuggestions.describe(0));
    assertEquals("", CashSuggestions.describe(-5));
  }

  @Test
  @DisplayName("Mọi gợi ý đều hợp lệ: tăng dần, đủ tiền và không thể đưa ít hơn")
  void moiGoiYdeuHopLe() {
    for (long total : new long[] {
        1_000L, 35_000L, 87_000L, 235_000L, 460_000L, 1_235_000L, 10_780_000L}) {
      List<Long> goiY = CashSuggestions.forTotal(total);

      assertTrue(goiY.size() <= CashSuggestions.MAX_SUGGESTIONS);
      for (int i = 0; i < goiY.size(); i++) {
        long amount = goiY.get(i);
        assertTrue(amount >= total, "Gợi ý " + amount + " nhỏ hơn tổng " + total);
        assertEquals(0L, amount % 1_000L, "Gợi ý " + amount + " không phải bội số của 1.000");
        if (i > 0) {
          assertTrue(goiY.get(i - 1) < amount, "Danh sách gợi ý phải tăng dần nghiêm ngặt");
        }

        List<Long> menhGia = CashSuggestions.breakdown(amount);
        assertFalse(menhGia.isEmpty(), "Không phân rã được gợi ý " + amount);
        assertEquals(amount, menhGia.stream().mapToLong(Long::longValue).sum());
        for (long note : menhGia) {
          assertTrue(
              amount - note < total, "Bỏ tờ " + note + " khỏi " + amount + " vẫn đủ " + total);
        }
      }
    }
  }
}
