package com.dtaq.retail.entity;

import java.time.LocalDate;
import java.util.Objects;

/** Tài khoản nhân viên, ánh xạ bảng "NhanVien". */
public class NhanVien {

  private String maNV;
  private String hoTen;
  private String soDienThoai;
  private String email;
  private LocalDate ngayVaoLam;
  private String tenDangNhap;
  private String matKhau;
  private LoaiQuyen loaiQuyen;
  private boolean trangThaiHoatDong;

  /** Tạo đối tượng rỗng. */
  public NhanVien() {}

  /**
   * Tạo đối tượng với đầy đủ giá trị.
   *
   * <p>Constructor chỉ gán trực tiếp, không kiểm tra ràng buộc.
   */
  public NhanVien(
      String maNV,
      String hoTen,
      String soDienThoai,
      String email,
      LocalDate ngayVaoLam,
      String tenDangNhap,
      String matKhau,
      LoaiQuyen loaiQuyen,
      boolean trangThaiHoatDong) {
    this.maNV = maNV;
    this.hoTen = hoTen;
    this.soDienThoai = soDienThoai;
    this.email = email;
    this.ngayVaoLam = ngayVaoLam;
    this.tenDangNhap = tenDangNhap;
    this.matKhau = matKhau;
    this.loaiQuyen = loaiQuyen;
    this.trangThaiHoatDong = trangThaiHoatDong;
  }

  /** Mã nhân viên; không thể thay đổi sau khi tạo. */
  public String maNV() {
    return maNV;
  }

  /** Mã nhân viên; không thể thay đổi sau khi tạo. */
  public String getMaNV() {
    return maNV;
  }

  public String hoTen() {
    return hoTen;
  }

  public String getHoTen() {
    return hoTen;
  }

  /** Gán họ tên; bắt buộc có giá trị. */
  public void setHoTen(String hoTen) {
    this.hoTen = RangBuoc.chuoiBatBuoc(hoTen, "hoTen");
  }

  public String soDienThoai() {
    return soDienThoai;
  }

  public String getSoDienThoai() {
    return soDienThoai;
  }

  /** Gán số điện thoại; phải khớp {@link RangBuoc#MAU_SO_DIEN_THOAI}. */
  public void setSoDienThoai(String soDienThoai) {
    this.soDienThoai = RangBuoc.chuoiKhopMau(
        soDienThoai, RangBuoc.MAU_SO_DIEN_THOAI, "soDienThoai", "Số điện thoại không hợp lệ");
  }

  public String email() {
    return email;
  }

  public String getEmail() {
    return email;
  }

  /** Gán email; cho phép để trống (null) nhưng nếu có phải đúng định dạng. */
  public void setEmail(String email) {
    this.email =
        RangBuoc.chuoiTuyChonKhopMau(email, RangBuoc.MAU_EMAIL, "email", "Email không hợp lệ");
  }

  public LocalDate ngayVaoLam() {
    return ngayVaoLam;
  }

  public LocalDate getNgayVaoLam() {
    return ngayVaoLam;
  }

  /** Gán ngày vào làm; bắt buộc có giá trị. */
  public void setNgayVaoLam(LocalDate ngayVaoLam) {
    this.ngayVaoLam = RangBuoc.batBuoc(ngayVaoLam, "ngayVaoLam");
  }

  public String tenDangNhap() {
    return tenDangNhap;
  }

  public String getTenDangNhap() {
    return tenDangNhap;
  }

  /** Gán tên đăng nhập; bắt buộc có giá trị (không kiểm tra duy nhất). */
  public void setTenDangNhap(String tenDangNhap) {
    this.tenDangNhap = RangBuoc.chuoiBatBuoc(tenDangNhap, "tenDangNhap");
  }

  public String matKhau() {
    return matKhau;
  }

  public String getMatKhau() {
    return matKhau;
  }

  /**
   * Gán mật khẩu; bắt buộc có giá trị.
   *
   * <p>Giá trị phải là mật khẩu đã băm (BCrypt) do tầng xác thực tạo ra. Entity không băm lại và
   * không ghi giá trị này ra log.
   */
  public void setMatKhau(String matKhau) {
    this.matKhau = RangBuoc.chuoiBatBuoc(matKhau, "matKhau");
  }

  public LoaiQuyen loaiQuyen() {
    return loaiQuyen;
  }

  public LoaiQuyen getLoaiQuyen() {
    return loaiQuyen;
  }

  /** Gán loại quyền; bắt buộc có giá trị. */
  public void setLoaiQuyen(LoaiQuyen loaiQuyen) {
    this.loaiQuyen = RangBuoc.batBuoc(loaiQuyen, "loaiQuyen");
  }

  public boolean trangThaiHoatDong() {
    return trangThaiHoatDong;
  }

  public boolean getTrangThaiHoatDong() {
    return trangThaiHoatDong;
  }

  public boolean isTrangThaiHoatDong() {
    return trangThaiHoatDong;
  }

  public void setTrangThaiHoatDong(boolean trangThaiHoatDong) {
    this.trangThaiHoatDong = trangThaiHoatDong;
  }

  /** Returns a readable representation that never includes the password. */
  @Override
  public String toString() {
    return "NhanVien[maNV=" + maNV
        + ", hoTen=" + hoTen
        + ", tenDangNhap=" + tenDangNhap
        + ", loaiQuyen=" + loaiQuyen
        + ", trangThaiHoatDong=" + trangThaiHoatDong
        + "]";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NhanVien khac)) {
      return false;
    }
    return trangThaiHoatDong == khac.trangThaiHoatDong
        && Objects.equals(maNV, khac.maNV)
        && Objects.equals(hoTen, khac.hoTen)
        && Objects.equals(soDienThoai, khac.soDienThoai)
        && Objects.equals(email, khac.email)
        && Objects.equals(ngayVaoLam, khac.ngayVaoLam)
        && Objects.equals(tenDangNhap, khac.tenDangNhap)
        && Objects.equals(matKhau, khac.matKhau)
        && Objects.equals(loaiQuyen, khac.loaiQuyen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        maNV,
        hoTen,
        soDienThoai,
        email,
        ngayVaoLam,
        tenDangNhap,
        matKhau,
        loaiQuyen,
        trangThaiHoatDong);
  }

  /** Returns the Vietnamese display name of the employee role. */
  public String moTaQuyen() {
    return loaiQuyen == LoaiQuyen.NHAN_VIEN_QUAN_LY ? "Quản lý" : "Nhân viên bán hàng";
  }
}
