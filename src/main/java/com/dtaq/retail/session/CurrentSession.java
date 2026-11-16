package com.dtaq.retail.session;

import com.dtaq.retail.entity.NhanVien;
import java.util.Optional;

/** Holds the employee of the current login session. */
public final class CurrentSession {

  private static volatile NhanVien hienTai;

  private CurrentSession() {
  }

  /** Sets the logged-in employee. */
  public static void set(NhanVien nhanVien) {
    if (nhanVien == null) {
      throw new IllegalArgumentException("Nhân viên không được để trống");
    }
    hienTai = nhanVien;
  }

  /** Returns the logged-in employee if present. */
  public static Optional<NhanVien> get() {
    return Optional.ofNullable(hienTai);
  }

  /** Returns the logged-in employee or fails when no session exists. */
  public static NhanVien require() {
    NhanVien nhanVien = hienTai;
    if (nhanVien == null) {
      throw new IllegalStateException("Chưa có phiên đăng nhập");
    }
    return nhanVien;
  }

  /** Clears the current session. */
  /**
   * Yêu cầu có phiên đăng nhập của tài khoản đang hoạt động (XBR-13: kiểm tại nơi xử lý nghiệp vụ).
   *
   * @return nhân viên của phiên hiện tại
   * @throws SecurityException nếu chưa đăng nhập hoặc tài khoản đã bị khóa
   */
  public static NhanVien yeuCauDangHoatDong() {
    NhanVien nhanVien = hienTai;
    if (nhanVien == null || !nhanVien.trangThaiHoatDong()) {
      throw new SecurityException("Cần đăng nhập bằng tài khoản đang hoạt động để thực hiện thao tác");
    }
    return nhanVien;
  }

  public static void clear() {
    hienTai = null;
  }

  /** Returns true when a session is active. */
  public static boolean isAuthenticated() {
    return hienTai != null;
  }
}
