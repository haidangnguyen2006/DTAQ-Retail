package com.dtaq.retail.entity;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Một bản sao lưu/phục hồi cơ sở dữ liệu, ánh xạ bảng "LichSuSaoLuu".
 *
 * <p>Bản ghi chỉ là cache hiển thị: nguồn sự thật của một bản sao lưu là cặp tệp
 * {@code *.dump} + {@code *.meta.json} trong thư mục backup. {@code sha256} và {@code fileSize}
 * dùng để kiểm tra nhanh tệp còn khớp metadata hay không.
 *
 * @param id khóa kỹ thuật do cơ sở dữ liệu sinh; {@code null} khi chưa lưu
 * @param filePath đường dẫn tệp dump; không được {@code null}
 * @param fileSize kích thước tệp dump tính bằng byte
 * @param sha256 mã băm SHA-256 dạng hex thường (64 ký tự), có thể {@code null} khi thất bại
 * @param loai loại thao tác; không được {@code null}
 * @param trangThai kết quả thao tác; không được {@code null}
 * @param thongBao thông điệp lỗi hoặc ghi chú hiển thị cho người dùng
 * @param createdAt thời điểm tạo bản ghi; không được {@code null}
 * @param createdBy người hoặc tiến trình thực hiện
 * @param phienBanPg phiên bản {@code pg_dump} đã tạo tệp
 * @param tenCsdl tên cơ sở dữ liệu đã sao lưu
 */
public record BackupRecord(
    Long id,
    Path filePath,
    long fileSize,
    String sha256,
    LoaiSaoLuu loai,
    TrangThaiSaoLuu trangThai,
    String thongBao,
    Instant createdAt,
    String createdBy,
    String phienBanPg,
    String tenCsdl) {

  /** Loại thao tác sao lưu/phục hồi. */
  public enum LoaiSaoLuu {
    AUTO,
    MANUAL,
    SAFETY,
    AUTO_RESTORE,
    MANUAL_RESTORE
  }

  /** Kết quả của một thao tác sao lưu/phục hồi. */
  public enum TrangThaiSaoLuu {
    SUCCESS,
    FAILED
  }

  /**
   * Kiểm tra các trường bắt buộc của một bản ghi.
   *
   * @throws NullPointerException nếu {@code filePath}, {@code loai}, {@code trangThai} hoặc
   *     {@code createdAt} là {@code null}
   */
  public BackupRecord {
    Objects.requireNonNull(filePath, "filePath");
    Objects.requireNonNull(loai, "loai");
    Objects.requireNonNull(trangThai, "trangThai");
    Objects.requireNonNull(createdAt, "createdAt");
  }

  /**
   * Lấy tên tệp của bản sao lưu, không kèm thư mục.
   *
   * @return tên tệp, ví dụ {@code dtaq_2026-11-05_000000.dump}
   */
  public String fileName() {
    Path ten = filePath.getFileName();
    return ten == null ? filePath.toString() : ten.toString();
  }

  /**
   * Cho biết thao tác đã thành công hay chưa.
   *
   * @return {@code true} khi trạng thái là {@link TrangThaiSaoLuu#SUCCESS}
   */
  public boolean thanhCong() {
    return trangThai == TrangThaiSaoLuu.SUCCESS;
  }
}
