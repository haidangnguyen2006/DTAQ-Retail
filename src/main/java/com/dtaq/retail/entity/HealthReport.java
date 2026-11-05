package com.dtaq.retail.entity;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Kết quả một lần kiểm tra sức khỏe dữ liệu.
 *
 * <p>Báo cáo là ảnh chụp tại thời điểm {@code thoiDiem}: số dòng hiện tại của từng bảng cốt lõi,
 * baseline đã lưu, tỷ lệ suy giảm phát hiện được, danh sách bảng còn thiếu và SQLState nếu lần
 * kiểm tra thất bại. Các tập {@code null} được chuẩn hóa thành tập rỗng bất biến để nơi nhận không
 * phải kiểm tra riêng.
 *
 * @param trangThai trạng thái tổng hợp; không được {@code null}
 * @param soDong số dòng hiện tại theo tên bảng
 * @param baseline số dòng baseline theo tên bảng
 * @param tyLeGiam tỷ lệ suy giảm theo tên bảng, trong khoảng {@code (0, 1]}
 * @param bangThieu các bảng cốt lõi không tồn tại trong schema {@code public}
 * @param chiTiet mô tả ngắn gọn phục vụ nhật ký và thông báo
 * @param sqlState mã SQLState của lỗi gần nhất, {@code null} khi kiểm tra thành công
 * @param thoiDiem thời điểm bắt đầu kiểm tra; không được {@code null}
 */
public record HealthReport(
    HealthStatus trangThai,
    Map<String, Long> soDong,
    Map<String, Long> baseline,
    Map<String, Double> tyLeGiam,
    List<String> bangThieu,
    String chiTiet,
    String sqlState,
    Instant thoiDiem) {

  /**
   * Kiểm tra trường bắt buộc và chuẩn hóa các tập dữ liệu.
   *
   * @throws NullPointerException nếu {@code trangThai} hoặc {@code thoiDiem} là {@code null}
   */
  public HealthReport {
    Objects.requireNonNull(trangThai, "trangThai");
    Objects.requireNonNull(thoiDiem, "thoiDiem");
    soDong = soDong == null
        ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(soDong));
    baseline = baseline == null
        ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(baseline));
    tyLeGiam = tyLeGiam == null
        ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(tyLeGiam));
    bangThieu = bangThieu == null ? List.of() : List.copyOf(bangThieu);
  }

  /**
   * Cho biết báo cáo có dấu hiệu bất thường cần can thiệp hay không.
   *
   * @return {@code true} khi trạng thái là {@link HealthStatus#SUSPICIOUS} hoặc
   *     {@link HealthStatus#CRITICAL}
   */
  public boolean coBatThuong() {
    return trangThai == HealthStatus.SUSPICIOUS || trangThai == HealthStatus.CRITICAL;
  }
}
