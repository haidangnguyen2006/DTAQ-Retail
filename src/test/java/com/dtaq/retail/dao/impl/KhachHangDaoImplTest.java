package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.util.MaGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link KhachHangDaoImpl} trên cơ sở dữ liệu phát triển. */
class KhachHangDaoImplTest {

  private static final String MA_KH_MAU = "KH-2026-000001";
  private static final String SDT_MAU = "0902100001";
  /** Mã chỉ dùng cho kiểm thử, ở năm 9999 để không ảnh hưởng số thứ tự của năm thật. */
  private static final String MA_KH_KIEM_THU = "KH-9999-999901";

  private KhachHangDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new KhachHangDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("Tìm khách theo SĐT trả về đúng hồ sơ")
  void findBySdtReturnsKhachHang() {
    Optional<KhachHang> found = dao.findBySdt(SDT_MAU);
    Assumptions.assumeTrue(found.isPresent(), "seed V4 chưa được áp dụng");

    KhachHang khachHang = found.orElseThrow();
    assertEquals(MA_KH_MAU, khachHang.maKH());
    assertEquals(SDT_MAU, khachHang.soDienThoai());
    assertFalse(khachHang.hoTen().isBlank(), "họ tên khách hàng phải có");
  }

  @Test
  @DisplayName("Tìm khách với SĐT không tồn tại trả về rỗng")
  void findBySdtUnknownIsEmpty() {
    assertTrue(dao.findBySdt("0999999999999").isEmpty());
  }

  @Test
  @DisplayName("Liệt kê khách hàng sắp xếp theo mã")
  void findAllReturnsSortedKhachHang() {
    List<KhachHang> ketQua = dao.findAll();
    Assumptions.assumeFalse(ketQua.isEmpty(), "seed V4 chưa được áp dụng");

    assertTrue(ketQua.stream().anyMatch(kh -> MA_KH_MAU.equals(kh.maKH())),
        "danh sách phải chứa khách hàng mẫu");
    for (int i = 1; i < ketQua.size(); i++) {
      assertTrue(ketQua.get(i - 1).maKH().compareTo(ketQua.get(i).maKH()) < 0,
          "kết quả phải sắp xếp theo mã khách hàng");
    }
  }

  @Test
  @DisplayName("Ghi khách hàng trong giao dịch của bên gọi rồi quay lui")
  void insertWritesKhachHangInsideCallerTransaction() throws SQLException {
    Connection ketNoi = ConnectDB.getDataSource().getConnection();
    try {
      ketNoi.setAutoCommit(false);
      String maKH = MaGenerator.next(ketNoi, "KhachHang", "maKH", "KH");
      KhachHang khachHang = new KhachHang(maKH, "Khách kiểm thử", "0988000001", null);

      dao.insert(ketNoi, khachHang);

      assertEquals(1, demKhachHang(ketNoi, maKH), "khách hàng phải tồn tại trong giao dịch");
      assertTrue(maKH.matches("^KH-\\d{4}-\\d{6}$"), "mã khách hàng sai định dạng: " + maKH);
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
  @DisplayName("Ghi khách hàng bằng kết nối riêng lưu được và tìm lại được theo SĐT")
  void insertWithOwnConnectionPersists() throws SQLException {
    String sdt = "0988000002";
    try {
      dao.insert(new KhachHang(MA_KH_KIEM_THU, "Khách kiểm thử", sdt, "1 Đường Thử"));

      Optional<KhachHang> found = dao.findBySdt(sdt);
      assertTrue(found.isPresent(), "khách hàng vừa lưu phải tìm được");
      assertEquals(MA_KH_KIEM_THU, found.orElseThrow().maKH());
      assertEquals("1 Đường Thử", found.orElseThrow().diaChi());
    } finally {
      xoaKhachHangKiemThu();
    }
  }

  @Test
  @DisplayName("Ghi khách hàng không có mã bị từ chối")
  void insertRejectsBlankMa() {
    assertThrows(IllegalArgumentException.class,
        () -> dao.insert(new KhachHang(" ", "Khách", "0988000003", null)));
    assertThrows(IllegalArgumentException.class, () -> dao.insert(null));
  }

  @Test
  @DisplayName("Cập nhật địa chỉ khách hàng rồi khôi phục")
  void updateChangesAndRestoresKhachHang() {
    Optional<KhachHang> found = dao.findBySdt(SDT_MAU);
    Assumptions.assumeTrue(found.isPresent(), "seed V4 chưa được áp dụng");
    KhachHang goc = found.orElseThrow();
    try {
      dao.update(new KhachHang(goc.maKH(), goc.hoTen(), goc.soDienThoai(), "99 Địa Chỉ Mới"));

      KhachHang moi = dao.findBySdt(SDT_MAU).orElseThrow();
      assertEquals(goc.maKH(), moi.maKH(), "mã khách hàng không được đổi");
      assertEquals("99 Địa Chỉ Mới", moi.diaChi());
    } finally {
      dao.update(goc);
    }
  }

  @Test
  @DisplayName("Cập nhật khách hàng không tồn tại báo lỗi")
  void updateUnknownThrows() {
    assertThrows(DataAccessException.class,
        () -> dao.update(new KhachHang("KH-9999-999999", "Không có", "0988000004", null)));
  }

  private int demKhachHang(Connection ketNoi, String maKH) throws SQLException {
    try (PreparedStatement statement =
        ketNoi.prepareStatement("SELECT COUNT(*) FROM \"KhachHang\" WHERE \"maKH\" = ?")) {
      statement.setString(1, maKH);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private void xoaKhachHangKiemThu() throws SQLException {
    try (Connection ketNoi = ConnectDB.getDataSource().getConnection();
        PreparedStatement statement =
            ketNoi.prepareStatement("DELETE FROM \"KhachHang\" WHERE \"maKH\" = ?")) {
      statement.setString(1, MA_KH_KIEM_THU);
      statement.executeUpdate();
    }
  }
}
