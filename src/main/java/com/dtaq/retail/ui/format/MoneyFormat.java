package com.dtaq.retail.ui.format;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Định dạng tiền tệ theo quy chuẩn Việt Nam (phân cách hàng nghìn bằng dấu chấm '.').
 * CHỈ phục vụ hiển thị, TUYỆT ĐỐI KHÔNG làm tròn hay thay đổi giá trị số học.
 */
public final class MoneyFormat {

  private static final DecimalFormatSymbols SYMBOLS;
  private static final DecimalFormat FORMATTER;

  static {
    SYMBOLS = new DecimalFormatSymbols(Locale.of("vi", "VN"));
    SYMBOLS.setGroupingSeparator('.');
    SYMBOLS.setDecimalSeparator(',');
    FORMATTER = new DecimalFormat("#,##0", SYMBOLS);
  }

  private MoneyFormat() {
  }

  /**
   * Định dạng số tiền thành chuỗi có phân cách hàng nghìn (ví dụ: 8000000 -> "8.000.000").
   */
  public static synchronized String format(long amount) {
    return FORMATTER.format(amount);
  }

  /**
   * Định dạng số tiền từ {@link BigDecimal}.
   */
  public static synchronized String format(BigDecimal amount) {
    if (amount == null) {
      return "0";
    }
    return FORMATTER.format(amount);
  }

  /** Số chữ số tối đa chấp nhận khi người dùng nhập số tiền (tránh tràn {@code long}). */
  public static final int MAX_INPUT_DIGITS = 15;

  /**
   * Đọc số tiền người dùng nhập: bỏ mọi ký tự không phải chữ số (dấu chấm phân cách, "VNĐ"…),
   * tối đa {@value #MAX_INPUT_DIGITS} chữ số. Chuỗi rỗng hoặc không có chữ số cho kết quả 0.
   * Chỉ phục vụ nhập liệu hiển thị; không làm tròn hay tính toán nghiệp vụ.
   */
  public static long parseDigits(String text) {
    if (text == null) {
      return 0L;
    }
    StringBuilder digits = new StringBuilder();
    for (int i = 0; i < text.length() && digits.length() < MAX_INPUT_DIGITS; i++) {
      char ch = text.charAt(i);
      if (ch >= '0' && ch <= '9') {
        digits.append(ch);
      }
    }
    return digits.length() == 0 ? 0L : Long.parseLong(digits.toString());
  }

  /**
   * Định dạng kèm hậu tố " VNĐ" (ví dụ: "8.000.000 VNĐ").
   */
  public static String formatVnd(long amount) {
    return format(amount) + " VNĐ";
  }

  /**
   * Định dạng kèm hậu tố " VNĐ" từ {@link BigDecimal}.
   */
  public static String formatVnd(BigDecimal amount) {
    return format(amount) + " VNĐ";
  }
}
