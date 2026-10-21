package com.dtaq.retail.entity;

import java.time.LocalDate;

/** Employee account record. */
public record NhanVien(
    String maNV,
    String hoTen,
    String soDienThoai,
    String email,
    LocalDate ngayVaoLam,
    String tenDangNhap,
    String matKhau,
    LoaiQuyen loaiQuyen,
    boolean trangThaiHoatDong) {

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

  /** Returns the Vietnamese display name of the employee role. */
  public String moTaQuyen() {
    return loaiQuyen == LoaiQuyen.NHAN_VIEN_QUAN_LY ? "Quản lý" : "Nhân viên bán hàng";
  }
}
