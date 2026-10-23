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
