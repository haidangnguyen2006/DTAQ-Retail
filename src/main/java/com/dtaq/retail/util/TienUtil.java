package com.dtaq.retail.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Điểm làm tròn và định dạng tiền tệ DUY NHẤT của dự án (quyết định C-11).
 *
 * <p>Quy chuẩn: VND, 0 chữ số thập phân, làm tròn {@link RoundingMode#HALF_UP}, chỉ dùng {@link
 * BigDecimal}. Mọi phép tính tiền phải đi qua lớp này; KHÔNG được tự làm tròn ở tầng khác hay tính
 * toán trên chuỗi đã hiển thị.
 */
public final class TienUtil {

  private static final DecimalFormatSymbols SYMBOLS = new DecimalFormatSymbols(Locale.of("vi", "VN"));
  private static final DecimalFormat FORMATTER = new DecimalFormat("#,##0", SYMBOLS);

  static {
    FORMATTER.setRoundingMode(RoundingMode.HALF_UP);
  }

  private TienUtil() {
  }

  /**
   * Làm tròn số tiền về VND, 0 chữ số thập phân, theo {@link RoundingMode#HALF_UP}.
   *
   * @param soTien số tiền cần làm tròn, không được {@code null}
   * @return số tiền đã làm tròn với scale 0
   * @throws IllegalArgumentException nếu {@code soTien} là {@code null}
   */
  public static BigDecimal lamTron(BigDecimal soTien) {
    if (soTien == null) {
      throw new IllegalArgumentException("Số tiền không được để trống");
    }
    return soTien.setScale(0, RoundingMode.HALF_UP);
  }

  /**
   * Làm tròn rồi định dạng số tiền thành chuỗi có phân cách hàng nghìn kèm hậu tố VND (ví dụ:
   * {@code "1.250.000 VND"}).
   *
   * @param soTien số tiền cần định dạng, không được {@code null}
   * @return chuỗi tiền tệ đã làm tròn và định dạng
   * @throws IllegalArgumentException nếu {@code soTien} là {@code null}
   */
  public static String dinhDang(BigDecimal soTien) {
    BigDecimal daLamTron = lamTron(soTien);
    synchronized (FORMATTER) {
      return FORMATTER.format(daLamTron) + " VND";
    }
  }

  /** Định dạng số tiền dạng {@code long}; xem {@link #dinhDang(BigDecimal)}. */
  public static String dinhDang(long soTien) {
    return dinhDang(BigDecimal.valueOf(soTien));
  }
}
