package com.dtaq.retail.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * Một sự cố sức khỏe dữ liệu do HealthMonitor phát hiện, ánh xạ bảng {@code "SuCoSucKhoe"}.
 *
 * <p>Bảng này là cache hiển thị/audit: nguồn sự thật là nhật ký ứng dụng, nên bản ghi có thể bị
 * dựng lại mà không ảnh hưởng tới dữ liệu nghiệp vụ. Một sự cố {@code PENDING} là sự cố đang chờ
 * Quản lý xử lý; các trạng thái còn lại là kết thúc.
 *
 * @param id khóa kỹ thuật do cơ sở dữ liệu sinh; {@code null} khi chưa lưu
 * @param severity mức độ nghiêm trọng; không được {@code null}
 * @param detail mô tả chi tiết phục vụ điều tra
 * @param trangThai trạng thái xử lý; không được {@code null}
 * @param backupUsed đường dẫn tệp sao lưu đã dùng để phục hồi, nếu có
 * @param detectedAt thời điểm phát hiện sự cố; không được {@code null}
 * @param resolvedAt thời điểm kết thúc xử lý; {@code null} khi còn {@code PENDING}
 * @param resolvedBy người hoặc tiến trình kết thúc sự cố, ví dụ {@code SYSTEM}
 */
public record HealthIncident(
    Long id,
    Severity severity,
    String detail,
    TrangThai trangThai,
    String backupUsed,
    Instant detectedAt,
    Instant resolvedAt,
    String resolvedBy) {

  /** Mức độ nghiêm trọng của sự cố sức khỏe. */
  public enum Severity {
    /** Dữ liệu hỏng hoặc mất bảng: cần phục hồi. */
    CRITICAL,
    /** Số dòng giảm bất thường: cần Quản lý xác nhận. */
    SUSPICIOUS
  }

  /** Trạng thái xử lý của sự cố sức khỏe. */
  public enum TrangThai {
    /** Đang chờ Quản lý xử lý. */
    PENDING,
    /** Đã phục hồi thủ công. */
    RESOLVED_RESTORED,
    /** Đã tự động phục hồi. */
    AUTO_RESTORED,
    /** Quản lý đã bỏ qua sự cố. */
    DISMISSED,
    /** Không thể phục hồi. */
    FAILED
  }

  /**
   * Kiểm tra các trường bắt buộc của một sự cố.
   *
   * @throws NullPointerException nếu {@code severity}, {@code trangThai} hoặc {@code detectedAt}
   *     là {@code null}
   */
  public HealthIncident {
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(trangThai, "trangThai");
    Objects.requireNonNull(detectedAt, "detectedAt");
  }

  /**
   * Cho biết sự cố còn đang chờ xử lý hay không.
   *
   * @return {@code true} khi trạng thái là {@link TrangThai#PENDING}
   */
  public boolean dangCho() {
    return trangThai == TrangThai.PENDING;
  }
}
