package com.dtaq.retail.entity;

import java.time.Instant;
import java.util.Objects;

/**
 * Một sự cố sức khỏe dữ liệu do HealthMonitor phát hiện, ánh xạ bảng {@code "SuCoSucKhoe"}.
 *
 * <p>Bảng này là cache hiển thị/audit: nguồn sự thật là nhật ký ứng dụng, nên bản ghi có thể bị
 * dựng lại mà không ảnh hưởng tới dữ liệu nghiệp vụ. Một sự cố {@code PENDING} là sự cố đang chờ
 * Quản lý xử lý; các trạng thái còn lại là kết thúc.
 */
public class HealthIncident {

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

  /** Khóa kỹ thuật do cơ sở dữ liệu sinh; {@code null} khi chưa lưu. */
  private Long id;

  /** Mức độ nghiêm trọng; không được {@code null}. */
  private Severity severity;

  /** Mô tả chi tiết phục vụ điều tra. */
  private String detail;

  /** Trạng thái xử lý; không được {@code null}. */
  private TrangThai trangThai;

  /** Đường dẫn tệp sao lưu đã dùng để phục hồi, nếu có. */
  private String backupUsed;

  /** Thời điểm phát hiện sự cố; không được {@code null}. */
  private Instant detectedAt;

  /** Thời điểm kết thúc xử lý; {@code null} khi còn {@code PENDING}. */
  private Instant resolvedAt;

  /** Người hoặc tiến trình kết thúc sự cố, ví dụ {@code SYSTEM}. */
  private String resolvedBy;

  /** Tạo sự cố rỗng dùng cho ánh xạ dữ liệu. */
  public HealthIncident() {}

  /**
   * Tạo sự cố đầy đủ.
   *
   * @param id khóa kỹ thuật do cơ sở dữ liệu sinh; {@code null} khi chưa lưu
   * @param severity mức độ nghiêm trọng; không được {@code null}
   * @param detail mô tả chi tiết phục vụ điều tra
   * @param trangThai trạng thái xử lý; không được {@code null}
   * @param backupUsed đường dẫn tệp sao lưu đã dùng để phục hồi, nếu có
   * @param detectedAt thời điểm phát hiện sự cố; không được {@code null}
   * @param resolvedAt thời điểm kết thúc xử lý; {@code null} khi còn {@code PENDING}
   * @param resolvedBy người hoặc tiến trình kết thúc sự cố, ví dụ {@code SYSTEM}
   * @throws NullPointerException nếu {@code severity}, {@code trangThai} hoặc {@code detectedAt}
   *     là {@code null}
   */
  public HealthIncident(
      Long id,
      Severity severity,
      String detail,
      TrangThai trangThai,
      String backupUsed,
      Instant detectedAt,
      Instant resolvedAt,
      String resolvedBy) {
    Objects.requireNonNull(severity, "severity");
    Objects.requireNonNull(trangThai, "trangThai");
    Objects.requireNonNull(detectedAt, "detectedAt");
    this.id = id;
    this.severity = severity;
    this.detail = detail;
    this.trangThai = trangThai;
    this.backupUsed = backupUsed;
    this.detectedAt = detectedAt;
    this.resolvedAt = resolvedAt;
    this.resolvedBy = resolvedBy;
  }

  /**
   * Lấy khóa kỹ thuật do cơ sở dữ liệu sinh.
   *
   * @return khóa kỹ thuật, hoặc {@code null} khi chưa lưu
   */
  public Long id() {
    return id;
  }

  /** @return khóa kỹ thuật, hoặc {@code null} khi chưa lưu */
  public Long getId() {
    return id;
  }

  /** @return mức độ nghiêm trọng */
  public Severity severity() {
    return severity;
  }

  /** @return mức độ nghiêm trọng */
  public Severity getSeverity() {
    return severity;
  }

  /**
   * Đặt mức độ nghiêm trọng.
   *
   * @throws NullPointerException nếu {@code severity} là {@code null}
   */
  public void setSeverity(Severity severity) {
    this.severity = RangBuoc.batBuoc(severity, "severity");
  }

  /** @return mô tả chi tiết phục vụ điều tra */
  public String detail() {
    return detail;
  }

  /** @return mô tả chi tiết phục vụ điều tra */
  public String getDetail() {
    return detail;
  }

  /** Đặt mô tả chi tiết phục vụ điều tra. */
  public void setDetail(String detail) {
    this.detail = RangBuoc.chuoiTuyChon(detail);
  }

  /** @return trạng thái xử lý */
  public TrangThai trangThai() {
    return trangThai;
  }

  /** @return trạng thái xử lý */
  public TrangThai getTrangThai() {
    return trangThai;
  }

  /**
   * Đặt trạng thái xử lý.
   *
   * @throws NullPointerException nếu {@code trangThai} là {@code null}
   */
  public void setTrangThai(TrangThai trangThai) {
    this.trangThai = RangBuoc.batBuoc(trangThai, "trangThai");
  }

  /** @return đường dẫn tệp sao lưu đã dùng để phục hồi, nếu có */
  public String backupUsed() {
    return backupUsed;
  }

  /** @return đường dẫn tệp sao lưu đã dùng để phục hồi, nếu có */
  public String getBackupUsed() {
    return backupUsed;
  }

  /** Đặt đường dẫn tệp sao lưu đã dùng để phục hồi. */
  public void setBackupUsed(String backupUsed) {
    this.backupUsed = RangBuoc.chuoiTuyChon(backupUsed);
  }

  /** @return thời điểm phát hiện sự cố */
  public Instant detectedAt() {
    return detectedAt;
  }

  /** @return thời điểm phát hiện sự cố */
  public Instant getDetectedAt() {
    return detectedAt;
  }

  /**
   * Đặt thời điểm phát hiện sự cố.
   *
   * @throws NullPointerException nếu {@code detectedAt} là {@code null}
   */
  public void setDetectedAt(Instant detectedAt) {
    this.detectedAt = RangBuoc.batBuoc(detectedAt, "detectedAt");
  }

  /** @return thời điểm kết thúc xử lý, hoặc {@code null} khi còn {@code PENDING} */
  public Instant resolvedAt() {
    return resolvedAt;
  }

  /** @return thời điểm kết thúc xử lý, hoặc {@code null} khi còn {@code PENDING} */
  public Instant getResolvedAt() {
    return resolvedAt;
  }

  /**
   * Đặt thời điểm kết thúc xử lý.
   *
   * @throws IllegalArgumentException nếu {@code resolvedAt} trước {@code detectedAt}
   */
  public void setResolvedAt(Instant resolvedAt) {
    this.resolvedAt = resolvedAt;
    if (detectedAt != null && resolvedAt != null && resolvedAt.isBefore(detectedAt)) {
      throw new IllegalArgumentException("resolvedAt không được trước detectedAt");
    }
  }

  /** @return người hoặc tiến trình kết thúc sự cố */
  public String resolvedBy() {
    return resolvedBy;
  }

  /** @return người hoặc tiến trình kết thúc sự cố */
  public String getResolvedBy() {
    return resolvedBy;
  }

  /** Đặt người hoặc tiến trình kết thúc sự cố. */
  public void setResolvedBy(String resolvedBy) {
    this.resolvedBy = RangBuoc.chuoiTuyChon(resolvedBy);
  }

  /**
   * Cho biết sự cố còn đang chờ xử lý hay không.
   *
   * @return {@code true} khi trạng thái là {@link TrangThai#PENDING}
   */
  public boolean dangCho() {
    return trangThai == TrangThai.PENDING;
  }

  /**
   * Cho biết sự cố còn đang chờ xử lý hay không.
   *
   * @return {@code true} khi trạng thái là {@link TrangThai#PENDING}
   */
  public boolean isDangCho() {
    return dangCho();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof HealthIncident khac)) {
      return false;
    }
    return Objects.equals(id, khac.id)
        && severity == khac.severity
        && Objects.equals(detail, khac.detail)
        && trangThai == khac.trangThai
        && Objects.equals(backupUsed, khac.backupUsed)
        && Objects.equals(detectedAt, khac.detectedAt)
        && Objects.equals(resolvedAt, khac.resolvedAt)
        && Objects.equals(resolvedBy, khac.resolvedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, severity, detail, trangThai, backupUsed, detectedAt, resolvedAt, resolvedBy);
  }

  @Override
  public String toString() {
    return "HealthIncident[id=" + id
        + ", severity=" + severity
        + ", detail=" + detail
        + ", trangThai=" + trangThai
        + ", backupUsed=" + backupUsed
        + ", detectedAt=" + detectedAt
        + ", resolvedAt=" + resolvedAt
        + ", resolvedBy=" + resolvedBy
        + "]";
  }
}
