package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.PasswordHasher;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Tests for {@link AuthController}. */
class AuthControllerTest {

  private static final String USERNAME = "NVQL2026001";
  private static final String PASSWORD = "12345678";
  private static final String WRONG_PASSWORD = "sai-mat-khau";
  private static final String MA_NV = "NVQUANLY-2026-000001";

  @Test
  void missingUsernameRejected() {
    FakeNhanVienDao dao = new FakeNhanVienDao();
    AuthController.LoginResult result = new AuthController(dao).login("   ", PASSWORD);

    assertEquals(AuthController.Outcome.MISSING_FIELDS, result.outcome());
    assertEquals(0, dao.soLanGoi);
  }

  @Test
  void missingPasswordRejected() {
    FakeNhanVienDao dao = new FakeNhanVienDao();
    AuthController.LoginResult result = new AuthController(dao).login(USERNAME, "   ");

    assertEquals(AuthController.Outcome.MISSING_FIELDS, result.outcome());
    assertEquals(0, dao.soLanGoi);
  }

  @Test
  void unknownUserRejected() {
    FakeNhanVienDao dao = new FakeNhanVienDao();
    dao.ketQua = Optional.empty();
    AuthController.LoginResult result = new AuthController(dao).login(USERNAME, PASSWORD);

    assertEquals(AuthController.Outcome.INVALID_CREDENTIALS, result.outcome());
    assertNull(result.nhanVien());
    assertFalse(result.thanhCong());
  }

  @Test
  void wrongPasswordRejected() {
    FakeNhanVienDao dao = new FakeNhanVienDao();
    dao.ketQua = Optional.of(employee(true));
    AuthController.LoginResult result = new AuthController(dao).login(USERNAME, WRONG_PASSWORD);

    assertEquals(AuthController.Outcome.INVALID_CREDENTIALS, result.outcome());
    assertNull(result.nhanVien());
  }

  @Test
  void lockedAccountRejected() {
    FakeNhanVienDao dao = new FakeNhanVienDao();
    dao.ketQua = Optional.of(employee(false));
    AuthController.LoginResult result = new AuthController(dao).login(USERNAME, PASSWORD);

    assertEquals(AuthController.Outcome.LOCKED, result.outcome());
    assertNull(result.nhanVien());
  }

  @Test
  void successReturnsEmployee() {
    FakeNhanVienDao dao = new FakeNhanVienDao();
    dao.ketQua = Optional.of(employee(true));
    AuthController.LoginResult result = new AuthController(dao).login(USERNAME, PASSWORD);

    assertEquals(AuthController.Outcome.SUCCESS, result.outcome());
    assertTrue(result.thanhCong());
    assertTrue(result.nhanVien() != null);
    assertEquals(MA_NV, result.nhanVien().maNV());
  }

  @Test
  void dataAccessErrorMapsToDataError() {
    FakeNhanVienDao dao = new FakeNhanVienDao();
    dao.loi = new DataAccessException("không thể truy vấn");
    AuthController.LoginResult result = new AuthController(dao).login(USERNAME, PASSWORD);

    assertEquals(AuthController.Outcome.DATA_ERROR, result.outcome());
    assertNull(result.nhanVien());
  }

  private static NhanVien employee(boolean active) {
    return new NhanVien(
        MA_NV,
        "Nguyễn Hải Đăng",
        "0901234567",
        "nguyenhaidangnt2006@gmail.com",
        LocalDate.of(2026, 1, 5),
        USERNAME,
        PasswordHasher.hash(PASSWORD.toCharArray()),
        LoaiQuyen.NHAN_VIEN_QUAN_LY,
        active);
  }

  private static final class FakeNhanVienDao implements NhanVienDao {

    private Optional<NhanVien> ketQua = Optional.empty();
    private RuntimeException loi;
    private int soLanGoi;

    @Override
    public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
      soLanGoi++;
      if (loi != null) {
        throw loi;
      }
      return ketQua;
    }

    @Override
    public void updateMatKhau(String maNV, String matKhauHash) {
      throw new UnsupportedOperationException("không dùng trong kiểm thử đăng nhập");
    }
  }
}
