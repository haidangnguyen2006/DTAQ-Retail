package com.dtaq.retail.ui.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Kiểm tra các bộ định dạng hiển thị UI")
class FormattersTest {

  @Test
  @DisplayName("MoneyFormat: Định dạng số tiền có phân cách hàng nghìn bằng dấu chấm")
  void testMoneyFormat() {
    assertEquals("0", MoneyFormat.format(0));
    assertEquals("8.000.000", MoneyFormat.format(8000000));
    assertEquals("246.000.000", MoneyFormat.format(246000000L));
    assertEquals("8.000.000 VNĐ", MoneyFormat.formatVnd(8000000));
    assertEquals("246.000.000 VNĐ", MoneyFormat.formatVnd(new BigDecimal("246000000")));
  }

  @Test
  @DisplayName("DateFormats: Định dạng ngày giờ chuẩn")
  void testDateFormats() {
    LocalDate d = LocalDate.of(2026, 10, 9);
    LocalDateTime dt = LocalDateTime.of(2026, 10, 9, 9, 42);

    assertEquals("09/10/2026", DateFormats.formatDate(d));
    assertEquals("09/10/2026, 09:42", DateFormats.formatDateTime(dt));
    assertEquals("09/10/2026 • 09:42", DateFormats.formatDateTimeBullet(dt));
    assertEquals("09/10", DateFormats.formatDayMonth(d));
    assertEquals("09:42", DateFormats.formatTime(dt));
  }

  @Test
  @DisplayName("PhoneFormat: Định dạng số điện thoại 10 số thành cụm 4-3-3")
  void testPhoneFormat() {
    assertEquals("0909 123 456", PhoneFormat.format("0909123456"));
    assertEquals("0909 123 456", PhoneFormat.format("0909-123-456"));
    assertEquals("19001000", PhoneFormat.format("19001000")); // không đổi nếu khác 10 số
    assertEquals("", PhoneFormat.format(null));
  }

  @Test
  @DisplayName("PersonNames: Chữ cái avatar (initials) và tên gọi ngắn gọn")
  void testPersonNames() {
    assertEquals("TH", PersonNames.initials("Lê Thu Hà"));
    assertEquals("ML", PersonNames.initials("Trần Mai Linh"));
    assertEquals("VA", PersonNames.initials("Nguyễn Văn A"));
    assertEquals("AD", PersonNames.initials("Admin"));

    assertEquals("Thu Hà", PersonNames.shortName("Lê Thu Hà"));
    assertEquals("Mai Linh", PersonNames.shortName("Trần Mai Linh"));
    assertEquals("Admin", PersonNames.shortName("Admin"));
  }

  @Test
  @DisplayName("MoneyFormat.parseDigits: đọc số tiền nhập, bỏ ký tự không phải chữ số")
  void testParseDigits() {
    assertEquals(0L, MoneyFormat.parseDigits(null));
    assertEquals(0L, MoneyFormat.parseDigits(""));
    assertEquals(0L, MoneyFormat.parseDigits("abc"));
    assertEquals(9_000_000L, MoneyFormat.parseDigits("9.000.000"));
    assertEquals(8_800_000L, MoneyFormat.parseDigits("8.800.000 VNĐ"));
    assertEquals(12L, MoneyFormat.parseDigits("0012"));
    // Giới hạn 15 chữ số để không tràn long
    assertEquals(123_456_789_012_345L, MoneyFormat.parseDigits("1234567890123456789"));
  }
}
