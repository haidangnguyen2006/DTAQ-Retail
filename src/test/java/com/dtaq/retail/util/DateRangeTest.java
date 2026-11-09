package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử kỳ báo cáo dùng chung: phân tích {@code dd/MM/yyyy} và kiểm tra tính hợp lệ. */
@DisplayName("DateRange - kỳ báo cáo dùng chung")
class DateRangeTest {

  @Test
  @DisplayName("Phân tích kỳ hợp lệ theo định dạng dd/MM/yyyy")
  void parseKyHopLe() {
    DateRange ky = DateRange.parse("01/01/2026", "31/01/2026");

    assertEquals(new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), ky);
  }

  @Test
  @DisplayName("Bỏ khoảng trắng thừa ở hai đầu trước khi phân tích")
  void parseBoKhoangTrang() {
    assertEquals(
        new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)),
        DateRange.parse(" 01/01/2026 ", " 31/01/2026 "));
  }

  @Test
  @DisplayName("Kỳ có ngày bắt đầu bằng ngày kết thúc là hợp lệ")
  void parseKyCungNgay() {
    assertEquals(
        new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)),
        DateRange.parse("01/01/2026", "01/01/2026"));
  }

  @Test
  @DisplayName("Ngày không tồn tại trong lịch bị từ chối")
  void parseNgayKhongTonTai() {
    assertThrows(
        IllegalArgumentException.class, () -> DateRange.parse("31/02/2026", "31/03/2026"));
  }

  @Test
  @DisplayName("Định dạng khác dd/MM/yyyy bị từ chối")
  void parseSaiDinhDang() {
    IllegalArgumentException loi =
        assertThrows(
            IllegalArgumentException.class, () -> DateRange.parse("2026-01-01", "31/01/2026"));

    assertEquals("Ngày không hợp lệ (định dạng dd/MM/yyyy): 2026-01-01", loi.getMessage());
  }

  @Test
  @DisplayName("Ngày bắt đầu null, rỗng hoặc chỉ khoảng trắng bị từ chối")
  void parseTuNgayTrong() {
    for (String giaTri : new String[] {null, "", "  "}) {
      IllegalArgumentException loi =
          assertThrows(
              IllegalArgumentException.class, () -> DateRange.parse(giaTri, "31/01/2026"));
      assertEquals("Từ ngày không được để trống", loi.getMessage());
    }
  }

  @Test
  @DisplayName("Ngày kết thúc null, rỗng hoặc chỉ khoảng trắng bị từ chối")
  void parseDenNgayTrong() {
    for (String giaTri : new String[] {null, "", "  "}) {
      IllegalArgumentException loi =
          assertThrows(
              IllegalArgumentException.class, () -> DateRange.parse("01/01/2026", giaTri));
      assertEquals("Đến ngày không được để trống", loi.getMessage());
    }
  }

  @Test
  @DisplayName("Kỳ đảo ngược bị từ chối với thông điệp nghiệp vụ")
  void parseKyDaoNguoc() {
    IllegalArgumentException loi =
        assertThrows(
            IllegalArgumentException.class, () -> DateRange.parse("31/01/2026", "01/01/2026"));

    assertEquals("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu", loi.getMessage());
  }

  @Test
  @DisplayName("Khởi tạo trực tiếp với ngày null bị từ chối")
  void taoTrucTiepNgayNull() {
    IllegalArgumentException loiTuNgay =
        assertThrows(IllegalArgumentException.class, () -> new DateRange(null, null));
    assertEquals("Từ ngày không được để trống", loiTuNgay.getMessage());

    IllegalArgumentException loiDenNgay =
        assertThrows(
            IllegalArgumentException.class,
            () -> new DateRange(LocalDate.of(2026, 1, 1), null));
    assertEquals("Đến ngày không được để trống", loiDenNgay.getMessage());
  }

  @Test
  @DisplayName("Khởi tạo trực tiếp với kỳ đảo ngược bị từ chối")
  void taoTrucTiepKyDaoNguoc() {
    IllegalArgumentException loi =
        assertThrows(
            IllegalArgumentException.class,
            () -> new DateRange(LocalDate.of(2026, 1, 31), LocalDate.of(2026, 1, 1)));

    assertEquals("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu", loi.getMessage());
  }
}
