package com.dtaq.retail.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Các hàm thuần áp chính sách giám sát sức khỏe: ngưỡng suy giảm, số chu kỳ xác nhận và giới hạn
 * tần suất tự động phục hồi.
 *
 * <p>Lớp không giữ trạng thái và không truy cập cơ sở dữ liệu để có thể kiểm thử độc lập với
 * {@link HealthStateStore} và dịch vụ sao lưu.
 */
public final class HealthPolicy {

  private HealthPolicy() {
  }

  /**
   * Cho biết mức dữ liệu hiện tại có đáng bị coi là bất thường so với baseline hay không.
   *
   * <p>Chỉ so sánh khi baseline đủ lớn; ngược lại bảng mới hoặc bảng nhỏ không bị coi là suy giảm.
   * Điều kiện là {@code current <= baseline * (1 - threshold)}.
   *
   * @param baseline số dòng baseline
   * @param current số dòng hiện tại
   * @param threshold tỷ lệ suy giảm tối thiểu, trong khoảng {@code (0, 1)}
   * @param minBaseline số dòng baseline tối thiểu để bắt đầu so sánh
   * @return {@code true} khi baseline đủ lớn và dữ liệu giảm ít nhất bằng ngưỡng
   */
  public static boolean nenSuspicious(long baseline, long current, double threshold,
      int minBaseline) {
    return baseline >= minBaseline && current <= baseline * (1 - threshold);
  }

  /**
   * Cho biết số chu kỳ bất thường liên tiếp đã đủ để xác nhận.
   *
   * @param streak số chu kỳ bất thường liên tiếp
   * @param cycles số chu kỳ cần có
   * @return {@code true} khi {@code streak >= cycles}
   */
  public static boolean duChuKy(int streak, int cycles) {
    return streak >= cycles;
  }

  /**
   * Lọc các lần tự động phục hồi còn nằm trong một giờ gần nhất.
   *
   * <p>Mốc {@code now - 1 giờ} được tính là còn hiệu lực.
   *
   * @param attempts danh sách thời điểm đã thử, có thể {@code null}
   * @param now mốc thời gian hiện tại
   * @return danh sách mới chỉ gồm các lần thử không cũ hơn một giờ
   */
  public static List<Instant> locTrongGio(List<Instant> attempts, Instant now) {
    Objects.requireNonNull(now, "now");
    if (attempts == null || attempts.isEmpty()) {
      return List.of();
    }
    Instant nguong = now.minus(1, ChronoUnit.HOURS);
    List<Instant> ketQua = new ArrayList<>(attempts.size());
    for (Instant attempt : attempts) {
      if (attempt != null && !attempt.isBefore(nguong)) {
        ketQua.add(attempt);
      }
    }
    return List.copyOf(ketQua);
  }

  /**
   * Cho biết còn được phép tự động phục hồi hay không.
   *
   * @param attempts danh sách thời điểm đã thử, có thể {@code null}
   * @param now mốc thời gian hiện tại
   * @param maxPerHour số lần tối đa trong một giờ
   * @return {@code true} khi số lần thử trong giờ gần nhất nhỏ hơn giới hạn
   */
  public static boolean duocAutoRestore(List<Instant> attempts, Instant now, int maxPerHour) {
    return locTrongGio(attempts, now).size() < maxPerHour;
  }
}
