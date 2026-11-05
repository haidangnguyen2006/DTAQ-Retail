package com.dtaq.retail.ui.format;

import java.util.Locale;

/**
 * Định dạng kích thước tệp cho giao diện theo đơn vị {@code B}, {@code KB}, {@code MB}, {@code GB}.
 *
 * <p>Chỉ phục vụ hiển thị: từ {@code KB} trở lên dùng một chữ số thập phân theo quy ước
 * {@code vi-VN} (dấu phẩy), ví dụ {@code 1536} hiển thị {@code "1,5 KB"}.
 */
public final class FileSizeFormat {

  private static final Locale VI = Locale.forLanguageTag("vi-VN");
  private static final long DON_VI = 1024L;
  private static final String[] TEN_DON_VI = {"B", "KB", "MB", "GB"};

  private FileSizeFormat() {
  }

  /**
   * Định dạng số byte thành chuỗi dễ đọc.
   *
   * @param bytes số byte; giá trị âm được hiểu là {@code 0}
   * @return chuỗi như {@code "512 B"}, {@code "1,0 KB"}, {@code "2,5 MB"}
   */
  public static String format(long bytes) {
    if (bytes <= 0) {
      return "0 B";
    }
    if (bytes < DON_VI) {
      return bytes + " B";
    }
    double giaTri = bytes;
    int chiSo = 0;
    while (giaTri >= DON_VI && chiSo < TEN_DON_VI.length - 1) {
      giaTri /= DON_VI;
      chiSo++;
    }
    return String.format(VI, "%.1f %s", giaTri, TEN_DON_VI[chiSo]);
  }
}
