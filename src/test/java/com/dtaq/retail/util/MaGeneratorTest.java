package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dtaq.retail.config.ConnectDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp {@link MaGenerator} trên cơ sở dữ liệu phát triển. */
class MaGeneratorTest {

  private Connection ketNoi;

  @BeforeEach
  void setUp() throws SQLException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    ketNoi = ConnectDB.getDataSource().getConnection();
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (ketNoi != null) {
      ketNoi.close();
    }
  }

  @Test
  @DisplayName("Năm chưa có dữ liệu thì mã bắt đầu từ 000001")
  void namChuaCoDuLieuBatDauTuMot() {
    assertEquals("HD-2030-000001", MaGenerator.next(ketNoi, "HoaDon", "maHD", "HD", 2030));
  }

  @Test
  @DisplayName("Năm đã có dữ liệu thì mã nối tiếp số lớn nhất")
  void noiTiepSoLonNhatTrongNam() {
    assertEquals("HD-2026-000041", MaGenerator.next(ketNoi, "HoaDon", "maHD", "HD", 2026));
  }

  @Test
  @DisplayName("Mã tăng dần trong cùng một giao dịch và quay lui không để lại dữ liệu")
  void tangDanTrongCungGiaoDich() throws SQLException {
    ketNoi.setAutoCommit(false);
    try {
      String id1 = MaGenerator.next(ketNoi, "KhachHang", "maKH", "KH", 2026);
      assertEquals("KH-2026-000061", id1);

      try (PreparedStatement statement = ketNoi.prepareStatement(
          "INSERT INTO \"KhachHang\" (\"maKH\", \"hoTen\", \"soDienThoai\") VALUES (?, ?, ?)")) {
        statement.setString(1, id1);
        statement.setString(2, "Kiểm thử");
        statement.setString(3, "0999999999");
        statement.executeUpdate();
      }

      String id2 = MaGenerator.next(ketNoi, "KhachHang", "maKH", "KH", 2026);
      assertEquals("KH-2026-000062", id2);
    } finally {
      ketNoi.rollback();
      ketNoi.setAutoCommit(true);
    }
  }

  @Test
  @DisplayName("Tên bảng chứa ký tự SQL bị từ chối")
  void tuChoiTenBangKhongHopLe() {
    assertThrows(IllegalArgumentException.class, () -> MaGenerator.next(
        ketNoi, "HoaDon; DROP TABLE \"HoaDon\"", "maHD", "HD", 2026));
  }

  @Test
  @DisplayName("Tiền tố chữ thường bị từ chối")
  void tuChoiTienToChuThuong() {
    assertThrows(IllegalArgumentException.class,
        () -> MaGenerator.next(ketNoi, "HoaDon", "maHD", "hd", 2026));
  }

  @Test
  @DisplayName("Kết nối null bị từ chối")
  void tuChoiKetNoiNull() {
    assertThrows(IllegalArgumentException.class,
        () -> MaGenerator.next(null, "HoaDon", "maHD", "HD", 2026));
  }
}
