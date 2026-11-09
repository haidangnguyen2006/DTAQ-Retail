package com.dtaq.retail.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/**
 * Kỳ báo cáo dùng chung cho các màn hình thống kê: một khoảng ngày đã được kiểm tra hợp lệ.
 *
 * <p>Kỳ luôn có đủ hai đầu và ngày bắt đầu không được sau ngày kết thúc, nhờ vậy tầng truy vấn không
 * phải kiểm tra lại. Chuỗi ngày của giao diện được chuyển sang {@link LocalDate} qua {@link
 * #parse(String, String)} với định dạng duy nhất của dự án là {@code dd/MM/yyyy}.
 *
 * @param tuNgay ngày bắt đầu của kỳ, không được {@code null}
 * @param denNgay ngày kết thúc của kỳ, không được {@code null} và không trước {@code tuNgay}
 */
public record DateRange(LocalDate tuNgay, LocalDate denNgay) {

  private static final DateTimeFormatter DINH_DANG =
      new DateTimeFormatterBuilder()
          .appendPattern("dd/MM/uuuu")
          .toFormatter()
          .withResolverStyle(ResolverStyle.STRICT);

  /**
   * Kiểm tra tính hợp lệ của kỳ ngay khi khởi tạo.
   *
   * @throws IllegalArgumentException nếu {@code tuNgay} hoặc {@code denNgay} là {@code null}, hoặc
   *     {@code tuNgay} sau {@code denNgay}
   */
  public DateRange {
    if (tuNgay == null) {
      throw new IllegalArgumentException("Từ ngày không được để trống");
    }
    if (denNgay == null) {
      throw new IllegalArgumentException("Đến ngày không được để trống");
    }
    if (tuNgay.isAfter(denNgay)) {
      throw new IllegalArgumentException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
    }
  }

  /**
   * Phân tích kỳ báo cáo từ hai chuỗi ngày do người dùng nhập, định dạng {@code dd/MM/yyyy}.
   *
   * <p>Chuỗi được cắt khoảng trắng hai đầu trước khi phân tích; ngày không tồn tại trong lịch (ví dụ
   * {@code 31/02/2026}) bị từ chối vì bộ phân tích chạy ở chế độ nghiêm ngặt.
   *
   * @param tuNgay chuỗi ngày bắt đầu, không được {@code null} hoặc rỗng
   * @param denNgay chuỗi ngày kết thúc, không được {@code null} hoặc rỗng
   * @return kỳ báo cáo đã kiểm tra hợp lệ
   * @throws IllegalArgumentException nếu một trong hai giá trị trống, sai định dạng {@code
   *     dd/MM/yyyy} hoặc kỳ bị đảo ngược
   */
  public static DateRange parse(String tuNgay, String denNgay) {
    if (tuNgay == null || tuNgay.isBlank()) {
      throw new IllegalArgumentException("Từ ngày không được để trống");
    }
    if (denNgay == null || denNgay.isBlank()) {
      throw new IllegalArgumentException("Đến ngày không được để trống");
    }
    return new DateRange(docNgay(tuNgay.trim()), docNgay(denNgay.trim()));
  }

  private static LocalDate docNgay(String giaTri) {
    try {
      return LocalDate.parse(giaTri, DINH_DANG);
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException("Ngày không hợp lệ (định dạng dd/MM/yyyy): " + giaTri, e);
    }
  }
}
