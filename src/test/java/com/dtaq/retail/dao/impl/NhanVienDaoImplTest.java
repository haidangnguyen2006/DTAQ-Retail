package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.MaGenerator;
import com.dtaq.retail.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
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

  @Test
  void findAllContainsSeededAccountsSortedByMa() {
    List<NhanVien> ketQua = dao.findAll();
    Assumptions.assumeTrue(
        ketQua.stream().anyMatch(nv -> SALES_MA_NV.equals(nv.maNV())), "seed V2 chưa được áp dụng");

    assertTrue(ketQua.stream().anyMatch(nv -> MANAGER_USERNAME.equals(nv.tenDangNhap())),
        "danh sách phải chứa tài khoản quản lý mẫu");
    for (int i = 1; i < ketQua.size(); i++) {
      assertTrue(ketQua.get(i - 1).maNV().compareTo(ketQua.get(i).maNV()) < 0,
          "kết quả phải sắp xếp theo mã nhân viên");
    }
  }

  @Test
  void findByMaReturnsEmployeeOrEmpty() {
    Optional<NhanVien> found = dao.findByMa(SALES_MA_NV);
    Assumptions.assumeTrue(found.isPresent(), "seed V2 chưa được áp dụng");
    assertEquals(SALES_USERNAME, found.orElseThrow().tenDangNhap());
    assertEquals(LoaiQuyen.NHAN_VIEN_BAN_HANG, found.orElseThrow().loaiQuyen());

    assertTrue(dao.findByMa("NVBANHANG-9999-999999").isEmpty());
  }

  @Test
  void existsChecksTenDangNhapAndMaNV() {
    Assumptions.assumeTrue(dao.findByMa(SALES_MA_NV).isPresent(), "seed V2 chưa được áp dụng");
    assertTrue(dao.existsTenDangNhap(SALES_USERNAME));
    assertFalse(dao.existsTenDangNhap("KHONG_TON_TAI_2026"));
    assertTrue(dao.existsMaNV(SALES_MA_NV));
    assertFalse(dao.existsMaNV("NVBANHANG-9999-999999"));
  }

  @Test
  void insertWritesEmployeeInsideCallerTransaction() throws SQLException {
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      ketNoi.setAutoCommit(false);
      String maNV = MaGenerator.next(ketNoi, "NhanVien", "maNV", "NVBANHANG");
      NhanVien nhanVien = new NhanVien(maNV, "Nhân viên kiểm thử", "0988000101", null,
          LocalDate.of(2026, 10, 29), "NVKIEMTHU_W0", PasswordHasher.hash("MatKhau2026".toCharArray()),
          LoaiQuyen.NHAN_VIEN_BAN_HANG, true);

      dao.insert(ketNoi, nhanVien);

      assertEquals(1, demNhanVien(ketNoi, maNV), "nhân viên phải tồn tại trong giao dịch");
      assertTrue(maNV.matches("^NVBANHANG-\\d{4}-\\d{6}$"), "mã nhân viên sai định dạng: " + maNV);
      assertFalse(ketNoi.getAutoCommit(), "DAO không được đổi auto-commit của bên gọi");
    } finally {
      try {
        ketNoi.rollback();
        ketNoi.setAutoCommit(true);
      } finally {
        ketNoi.close();
      }
    }
  }

  @Test
  void insertDuplicateTenDangNhapIsRejected() throws SQLException {
    Assumptions.assumeTrue(dao.existsTenDangNhap(SALES_USERNAME), "seed V2 chưa được áp dụng");
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      ketNoi.setAutoCommit(false);
      NhanVien trung = new NhanVien("NVBANHANG-9999-999901", "Trùng tên đăng nhập", "0988000102",
          null, LocalDate.of(2026, 10, 29), SALES_USERNAME,
          PasswordHasher.hash("MatKhau2026".toCharArray()), LoaiQuyen.NHAN_VIEN_BAN_HANG, true);

      assertThrows(DataAccessException.class, () -> dao.insert(ketNoi, trung));
    } finally {
      try {
        ketNoi.rollback();
        ketNoi.setAutoCommit(true);
      } finally {
        ketNoi.close();
      }
    }
  }

  @Test
  void insertRejectsBlankMa() throws SQLException {
    try (Connection ketNoi = ConnectDB.getDataSource().getConnection()) {
      assertThrows(IllegalArgumentException.class, () -> dao.insert(ketNoi, null));
    }
  }

  @Test
  void updateChangesProfileOnlyAndRestoresIt() {
    Optional<NhanVien> found = dao.findByMa(SALES_MA_NV);
    Assumptions.assumeTrue(found.isPresent(), "seed V2 chưa được áp dụng");
    NhanVien goc = found.orElseThrow();
    try {
      dao.update(new NhanVien(goc.maNV(), "Tên tạm kiểm thử", "0988000103", "tam@dtaq.example",
          goc.ngayVaoLam(), "TEN_KHAC", "hash-khac", LoaiQuyen.NHAN_VIEN_QUAN_LY, false));

      NhanVien moi = dao.findByMa(SALES_MA_NV).orElseThrow();
      assertEquals("Tên tạm kiểm thử", moi.hoTen());
      assertEquals("0988000103", moi.soDienThoai());
      assertEquals("tam@dtaq.example", moi.email());
      assertEquals(goc.tenDangNhap(), moi.tenDangNhap(), "tên đăng nhập không được đổi");
      assertEquals(goc.matKhau(), moi.matKhau(), "mật khẩu không được đổi");
      assertEquals(goc.loaiQuyen(), moi.loaiQuyen(), "loại quyền không được đổi");
      assertEquals(goc.trangThaiHoatDong(), moi.trangThaiHoatDong(), "trạng thái không được đổi");
    } finally {
      dao.update(goc);
    }
  }

  @Test
  void updateTrangThaiLocksAndUnlocksAccount() {
    Optional<NhanVien> found = dao.findByMa(SALES_MA_NV);
    Assumptions.assumeTrue(found.isPresent(), "seed V2 chưa được áp dụng");
    boolean goc = found.orElseThrow().trangThaiHoatDong();
    try {
      dao.updateTrangThai(SALES_MA_NV, false);
      assertFalse(dao.findByMa(SALES_MA_NV).orElseThrow().trangThaiHoatDong());

      dao.updateTrangThai(SALES_MA_NV, true);
      assertTrue(dao.findByMa(SALES_MA_NV).orElseThrow().trangThaiHoatDong());
    } finally {
      dao.updateTrangThai(SALES_MA_NV, goc);
    }
  }

  @Test
  void updateUnknownEmployeeThrows() {
    assertThrows(DataAccessException.class,
        () -> dao.updateTrangThai("NVBANHANG-9999-999999", false));
    assertThrows(DataAccessException.class, () -> dao.update(new NhanVien(
        "NVBANHANG-9999-999999", "Không có", "0988000104", null, LocalDate.of(2026, 1, 1),
        "KHONG_CO", "hash", LoaiQuyen.NHAN_VIEN_BAN_HANG, true)));
  }

  private int demNhanVien(Connection ketNoi, String maNV) throws SQLException {
    try (PreparedStatement statement =
        ketNoi.prepareStatement("SELECT COUNT(*) FROM \"NhanVien\" WHERE \"maNV\" = ?")) {
      statement.setString(1, maNV);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }
}
