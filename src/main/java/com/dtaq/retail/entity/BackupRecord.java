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
 */
public class BackupRecord {

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

  /** Khóa kỹ thuật do cơ sở dữ liệu sinh; {@code null} khi chưa lưu. */
  private Long id;

  /** Đường dẫn tệp dump; không được {@code null}. */
  private Path filePath;

  /** Kích thước tệp dump tính bằng byte. */
  private long fileSize;

  /** Mã băm SHA-256 dạng hex thường (64 ký tự), có thể {@code null} khi thất bại. */
  private String sha256;

  /** Loại thao tác; không được {@code null}. */
  private LoaiSaoLuu loai;

  /** Kết quả thao tác; không được {@code null}. */
  private TrangThaiSaoLuu trangThai;

  /** Thông điệp lỗi hoặc ghi chú hiển thị cho người dùng. */
  private String thongBao;

  /** Thời điểm tạo bản ghi; không được {@code null}. */
  private Instant createdAt;

  /** Người hoặc tiến trình thực hiện. */
  private String createdBy;

  /** Phiên bản {@code pg_dump} đã tạo tệp. */
  private String phienBanPg;

  /** Tên cơ sở dữ liệu đã sao lưu. */
  private String tenCsdl;

  /** Tạo bản ghi rỗng dùng cho ánh xạ dữ liệu. */
  public BackupRecord() {}

  /**
   * Tạo bản ghi đầy đủ.
   *
   * @param id khóa kỹ thuật do cơ sở dữ liệu sinh; {@code null} khi chưa lưu
   * @param filePath đường dẫn tệp dump; không được {@code null}
   * @param fileSize kích thước tệp dump tính bằng byte
   * @param sha256 mã băm SHA-256 dạng hex thường (64 ký tự), có thể {@code null}
   * @param loai loại thao tác; không được {@code null}
   * @param trangThai kết quả thao tác; không được {@code null}
   * @param thongBao thông điệp lỗi hoặc ghi chú hiển thị cho người dùng
   * @param createdAt thời điểm tạo bản ghi; không được {@code null}
   * @param createdBy người hoặc tiến trình thực hiện
   * @param phienBanPg phiên bản {@code pg_dump} đã tạo tệp
   * @param tenCsdl tên cơ sở dữ liệu đã sao lưu
   * @throws NullPointerException nếu {@code filePath}, {@code loai}, {@code trangThai} hoặc
   *     {@code createdAt} là {@code null}
   */
  public BackupRecord(
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
    Objects.requireNonNull(filePath, "filePath");
    Objects.requireNonNull(loai, "loai");
    Objects.requireNonNull(trangThai, "trangThai");
    Objects.requireNonNull(createdAt, "createdAt");
    this.id = id;
    this.filePath = filePath;
    this.fileSize = fileSize;
    this.sha256 = sha256;
    this.loai = loai;
    this.trangThai = trangThai;
    this.thongBao = thongBao;
    this.createdAt = createdAt;
    this.createdBy = createdBy;
    this.phienBanPg = phienBanPg;
    this.tenCsdl = tenCsdl;
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

  /** @return đường dẫn tệp dump */
  public Path filePath() {
    return filePath;
  }

  /** @return đường dẫn tệp dump */
  public Path getFilePath() {
    return filePath;
  }

  /**
   * Đặt đường dẫn tệp dump.
   *
   * @throws NullPointerException nếu {@code filePath} là {@code null}
   * @throws IllegalArgumentException nếu đường dẫn rỗng
   */
  public void setFilePath(Path filePath) {
    Objects.requireNonNull(filePath, "filePath");
    if (filePath.toString().isBlank()) {
      throw new IllegalArgumentException("filePath không được để trống");
    }
    this.filePath = filePath;
  }

  /** @return kích thước tệp dump tính bằng byte */
  public long fileSize() {
    return fileSize;
  }

  /** @return kích thước tệp dump tính bằng byte */
  public long getFileSize() {
    return fileSize;
  }

  /**
   * Đặt kích thước tệp dump.
   *
   * @throws IllegalArgumentException nếu {@code fileSize} âm
   */
  public void setFileSize(long fileSize) {
    if (fileSize < 0) {
      throw new IllegalArgumentException("fileSize không được âm");
    }
    this.fileSize = fileSize;
  }

  /** @return mã băm SHA-256 dạng hex thường, hoặc {@code null} */
  public String sha256() {
    return sha256;
  }

  /** @return mã băm SHA-256 dạng hex thường, hoặc {@code null} */
  public String getSha256() {
    return sha256;
  }

  /**
   * Đặt mã băm SHA-256.
   *
   * @throws IllegalArgumentException nếu khác {@code null} và không phải 64 ký tự hex thường
   */
  public void setSha256(String sha256) {
    this.sha256 = RangBuoc.chuoiTuyChonKhopMau(
        sha256, RangBuoc.MAU_SHA256, "sha256", "sha256 phải là 64 ký tự hex thường");
  }

  /** @return loại thao tác */
  public LoaiSaoLuu loai() {
    return loai;
  }

  /** @return loại thao tác */
  public LoaiSaoLuu getLoai() {
    return loai;
  }

  /**
   * Đặt loại thao tác.
   *
   * @throws NullPointerException nếu {@code loai} là {@code null}
   */
  public void setLoai(LoaiSaoLuu loai) {
    this.loai = RangBuoc.batBuoc(loai, "loai");
  }

  /** @return kết quả thao tác */
  public TrangThaiSaoLuu trangThai() {
    return trangThai;
  }

  /** @return kết quả thao tác */
  public TrangThaiSaoLuu getTrangThai() {
    return trangThai;
  }

  /**
   * Đặt kết quả thao tác.
   *
   * @throws NullPointerException nếu {@code trangThai} là {@code null}
   */
  public void setTrangThai(TrangThaiSaoLuu trangThai) {
    this.trangThai = RangBuoc.batBuoc(trangThai, "trangThai");
  }

  /** @return thông điệp lỗi hoặc ghi chú hiển thị */
  public String thongBao() {
    return thongBao;
  }

  /** @return thông điệp lỗi hoặc ghi chú hiển thị */
  public String getThongBao() {
    return thongBao;
  }

  /** Đặt thông điệp lỗi hoặc ghi chú hiển thị. */
  public void setThongBao(String thongBao) {
    this.thongBao = RangBuoc.chuoiTuyChon(thongBao);
  }

  /** @return thời điểm tạo bản ghi */
  public Instant createdAt() {
    return createdAt;
  }

  /** @return thời điểm tạo bản ghi */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Đặt thời điểm tạo bản ghi.
   *
   * @throws NullPointerException nếu {@code createdAt} là {@code null}
   */
  public void setCreatedAt(Instant createdAt) {
    this.createdAt = RangBuoc.batBuoc(createdAt, "createdAt");
  }

  /** @return người hoặc tiến trình thực hiện */
  public String createdBy() {
    return createdBy;
  }

  /** @return người hoặc tiến trình thực hiện */
  public String getCreatedBy() {
    return createdBy;
  }

  /** Đặt người hoặc tiến trình thực hiện. */
  public void setCreatedBy(String createdBy) {
    this.createdBy = RangBuoc.chuoiTuyChon(createdBy);
  }

  /** @return phiên bản {@code pg_dump} đã tạo tệp */
  public String phienBanPg() {
    return phienBanPg;
  }

  /** @return phiên bản {@code pg_dump} đã tạo tệp */
  public String getPhienBanPg() {
    return phienBanPg;
  }

  /** Đặt phiên bản {@code pg_dump} đã tạo tệp. */
  public void setPhienBanPg(String phienBanPg) {
    this.phienBanPg = RangBuoc.chuoiTuyChon(phienBanPg);
  }

  /** @return tên cơ sở dữ liệu đã sao lưu */
  public String tenCsdl() {
    return tenCsdl;
  }

  /** @return tên cơ sở dữ liệu đã sao lưu */
  public String getTenCsdl() {
    return tenCsdl;
  }

  /** Đặt tên cơ sở dữ liệu đã sao lưu. */
  public void setTenCsdl(String tenCsdl) {
    this.tenCsdl = RangBuoc.chuoiTuyChon(tenCsdl);
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

  /**
   * Cho biết thao tác đã thành công hay chưa.
   *
   * @return {@code true} khi trạng thái là {@link TrangThaiSaoLuu#SUCCESS}
   */
  public boolean isThanhCong() {
    return thanhCong();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof BackupRecord khac)) {
      return false;
    }
    return fileSize == khac.fileSize
        && Objects.equals(id, khac.id)
        && Objects.equals(filePath, khac.filePath)
        && Objects.equals(sha256, khac.sha256)
        && loai == khac.loai
        && trangThai == khac.trangThai
        && Objects.equals(thongBao, khac.thongBao)
        && Objects.equals(createdAt, khac.createdAt)
        && Objects.equals(createdBy, khac.createdBy)
        && Objects.equals(phienBanPg, khac.phienBanPg)
        && Objects.equals(tenCsdl, khac.tenCsdl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, filePath, fileSize, sha256, loai, trangThai, thongBao, createdAt, createdBy,
        phienBanPg, tenCsdl);
  }

  @Override
  public String toString() {
    return "BackupRecord[id=" + id
        + ", filePath=" + filePath
        + ", fileSize=" + fileSize
        + ", sha256=" + sha256
        + ", loai=" + loai
        + ", trangThai=" + trangThai
        + ", thongBao=" + thongBao
        + ", createdAt=" + createdAt
        + ", createdBy=" + createdBy
        + ", phienBanPg=" + phienBanPg
        + ", tenCsdl=" + tenCsdl
        + "]";
  }
}
