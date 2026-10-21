package com.dtaq.retail.controller;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.PasswordHasher;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Xử lý nghiệp vụ đăng nhập, tách khỏi giao diện và lớp truy cập dữ liệu. */
public final class AuthController {

  private static final Logger log = LoggerFactory.getLogger(AuthController.class);

  private final NhanVienDao nhanVienDao;

  /**
   * Khởi tạo bộ điều khiển đăng nhập.
   *
   * @param nhanVienDao lớp truy cập tài khoản nhân viên; không được {@code null}
   */
  public AuthController(NhanVienDao nhanVienDao) {
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
  }

  /**
   * Kiểm tra thông tin đăng nhập và trả về kết quả tương ứng.
   *
   * @param tenDangNhap tên đăng nhập do người dùng nhập
   * @param matKhau mật khẩu thô do người dùng nhập
   * @return kết quả đăng nhập; không bao giờ {@code null}
   */
  public LoginResult login(String tenDangNhap, String matKhau) {
    if (tenDangNhap == null || tenDangNhap.trim().isEmpty()
        || matKhau == null || matKhau.trim().isEmpty()) {
      return new LoginResult(Outcome.MISSING_FIELDS, null);
    }

    Optional<NhanVien> timThay;
    try {
      timThay = nhanVienDao.findByTenDangNhap(tenDangNhap.trim());
    } catch (DataAccessException e) {
      log.warn("Không thể tra cứu tài khoản nhân viên khi đăng nhập (exception={})",
          e.getClass().getName());
      return new LoginResult(Outcome.DATA_ERROR, null);
    }

    if (timThay.isEmpty()) {
      return new LoginResult(Outcome.INVALID_CREDENTIALS, null);
    }

    NhanVien nhanVien = timThay.get();
    if (!PasswordHasher.verify(matKhau.toCharArray(), nhanVien.matKhau())) {
      return new LoginResult(Outcome.INVALID_CREDENTIALS, null);
    }

    if (!nhanVien.trangThaiHoatDong()) {
      return new LoginResult(Outcome.LOCKED, null);
    }

    return new LoginResult(Outcome.SUCCESS, nhanVien);
  }

  /** Kết quả của một lần đăng nhập. */
  public enum Outcome {
    SUCCESS,
    MISSING_FIELDS,
    INVALID_CREDENTIALS,
    LOCKED,
    DATA_ERROR
  }

  /** Kết quả đăng nhập kèm nhân viên khi thành công. */
  public record LoginResult(Outcome outcome, NhanVien nhanVien) {

    /** Returns true when the login succeeded. */
    public boolean thanhCong() {
      return outcome == Outcome.SUCCESS;
    }
  }
}
