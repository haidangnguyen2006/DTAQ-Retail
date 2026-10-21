package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.util.PasswordHasher;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link NhanVienDaoImpl} trên cơ sở dữ liệu phát triển. */
class NhanVienDaoImplTest {

  private static final String DEMO_PASSWORD = "12345678";
  private static final String MANAGER_USERNAME = "NVQL2026001";
  private static final String SALES_USERNAME = "NVBH2026001";
  private static final String SALES_MA_NV = "NVBANHANG-2026-001";

  private NhanVienDaoImpl dao;

  @BeforeAll
  static void requireDatabase() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
  }

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new NhanVienDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  void findExistingManagerReturnsRole() {
    Optional<NhanVien> found = dao.findByTenDangNhap(MANAGER_USERNAME);
    Assumptions.assumeTrue(found.isPresent(), "seed V2 chưa được áp dụng");
    NhanVien nhanVien = found.orElseThrow();
    assertEquals(LoaiQuyen.NHAN_VIEN_QUAN_LY, nhanVien.loaiQuyen());
    assertEquals(MANAGER_USERNAME, nhanVien.tenDangNhap());
    assertEquals("nguyenhaidangnt2006@gmail.com", nhanVien.email());
  }

  @Test
  void seededPasswordIsDemoPassword() {
    Optional<NhanVien> found = dao.findByTenDangNhap(MANAGER_USERNAME);
    Assumptions.assumeTrue(found.isPresent(), "seed V2 chưa được áp dụng");
    assertTrue(PasswordHasher.verify(
        DEMO_PASSWORD.toCharArray(), found.orElseThrow().matKhau()));
  }

  @Test
  void findUnknownUsernameIsEmpty() {
    Optional<NhanVien> found = dao.findByTenDangNhap("KHONG_TON_TAI_2026");
    assertFalse(found.isPresent());
  }

  @Test
  void updateMatKhauChangesHashAndRestoresIt() {
    Optional<NhanVien> found = dao.findByTenDangNhap(SALES_USERNAME);
    Assumptions.assumeTrue(found.isPresent(), "seed V2 chưa được áp dụng");
    String originalHash = found.orElseThrow().matKhau();
    char[] newPassword = "MatKhauTam2026".toCharArray();
    try {
      dao.updateMatKhau(SALES_MA_NV, PasswordHasher.hash(newPassword));
      Optional<NhanVien> updated = dao.findByTenDangNhap(SALES_USERNAME);
      assertTrue(updated.isPresent(), "tài khoản phải còn tồn tại sau khi đổi mật khẩu");
      assertFalse(originalHash.equals(updated.orElseThrow().matKhau()),
          "mật khẩu đã băm phải thay đổi");
      assertTrue(PasswordHasher.verify(newPassword, updated.orElseThrow().matKhau()));
    } finally {
      dao.updateMatKhau(SALES_MA_NV, originalHash);
    }
  }
}
