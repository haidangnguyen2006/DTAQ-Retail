package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử điểm làm tròn tiền tệ duy nhất của dự án (quyết định C-11). */
@DisplayName("TienUtil - làm tròn và định dạng tiền VND")
class TienUtilTest {

  @Test
  @DisplayName("Làm tròn 2.5 thành 3 (HALF_UP)")
  void lamTronHaiPhayNam() {
    assertEquals(new BigDecimal("3"), TienUtil.lamTron(new BigDecimal("2.5")));
  }

  @Test
  @DisplayName("Làm tròn 3.5 thành 4 (HALF_UP)")
  void lamTronBaPhayNam() {
    assertEquals(new BigDecimal("4"), TienUtil.lamTron(new BigDecimal("3.5")));
  }

  @Test
  @DisplayName("Làm tròn -2.5 thành -3 (HALF_UP ra xa số 0)")
  void lamTronAmHaiPhayNam() {
    assertEquals(new BigDecimal("-3"), TienUtil.lamTron(new BigDecimal("-2.5")));
  }

  @Test
  @DisplayName("Làm tròn 1250000.4 thành 1250000")
  void lamTronLamXuong() {
    assertEquals(new BigDecimal("1250000"), TienUtil.lamTron(new BigDecimal("1250000.4")));
  }

  @Test
  @DisplayName("Làm tròn 1250000.5 thành 1250001")
  void lamTronLamLen() {
    assertEquals(new BigDecimal("1250001"), TienUtil.lamTron(new BigDecimal("1250000.5")));
  }

  @Test
  @DisplayName("Làm tròn null ném IllegalArgumentException")
  void lamTronNullNemLoi() {
    assertThrows(IllegalArgumentException.class, () -> TienUtil.lamTron(null));
  }

  @Test
  @DisplayName("Định dạng 1250000 thành \"1.250.000 VND\"")
  void dinhDangSoTron() {
    assertEquals("1.250.000 VND", TienUtil.dinhDang(new BigDecimal("1250000")));
  }

  @Test
  @DisplayName("Định dạng 0L thành \"0 VND\"")
  void dinhDangKhong() {
    assertEquals("0 VND", TienUtil.dinhDang(0L));
  }

  @Test
  @DisplayName("Định dạng 1250000.6 thành \"1.250.001 VND\" (làm tròn trước khi định dạng)")
  void dinhDangLamTronTruoc() {
    assertEquals("1.250.001 VND", TienUtil.dinhDang(new BigDecimal("1250000.6")));
  }

  @Test
  @DisplayName("Định dạng null ném IllegalArgumentException")
  void dinhDangNullNemLoi() {
    assertThrows(IllegalArgumentException.class, () -> TienUtil.dinhDang((BigDecimal) null));
  }

  @Test
  @DisplayName("Mọi kết quả định dạng không âm có dạng số có phân cách hàng nghìn kèm hậu tố VND")
  void dinhDangDungMauChuoi() {
    long[] values = {0L, 1L, 999L, 1000L, 1250000L, 1234567890L};
    for (long value : values) {
      String text = TienUtil.dinhDang(value);
      assertTrue(text.matches("^[0-9.]+ VND$"), "Sai định dạng: " + text);
    }
  }
}
