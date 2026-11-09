package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử bất biến tồn kho RB37 trên cơ sở dữ liệu phát triển.
 *
 * <p>RB37: {@code DongSanPham.soLuongTon} phải bằng số bản ghi {@code SanPham} của dòng đó đang ở
 * trạng thái {@code SAN_SANG_BAN}. Lớp này chỉ đọc dữ liệu, không ghi và không tạo fixture.
 */
class TonKhoInvariantTest {

  @BeforeAll
  static void kiemTraCsdl() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
  }

  @Test
  @DisplayName("soLuongTon của mọi dòng phải bằng số máy SAN_SANG_BAN")
  void soLuongTonBangSoMaySanSangBan() throws SQLException {
    String sql =
        "SELECT d.\"maDongSP\" FROM \"DongSanPham\" d "
            + "WHERE d.\"soLuongTon\" <> ("
            + "SELECT count(*) FROM \"SanPham\" s "
            + "WHERE s.\"maDongSP\" = d.\"maDongSP\" AND s.\"trangThai\" = 'SAN_SANG_BAN') "
            + "ORDER BY d.\"maDongSP\"";

    List<String> dongLech = new ArrayList<>();
    try (Connection conn = ConnectDB.getDataSource().getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        dongLech.add(rs.getString(1));
      }
    }

    assertTrue(
        dongLech.isEmpty(),
        "RB37: soLuongTon khong bang so may SAN_SANG_BAN o cac dong: " + dongLech);
  }

  @Test
  @DisplayName("Tổng soLuongTon phải bằng tổng số máy SAN_SANG_BAN")
  void tongTonKhopTongMaySanSangBan() throws SQLException {
    try (Connection conn = ConnectDB.getDataSource().getConnection()) {
      long tongTon = dem(conn, "SELECT sum(\"soLuongTon\") FROM \"DongSanPham\"");
      long tongMay =
          dem(conn, "SELECT count(*) FROM \"SanPham\" WHERE \"trangThai\" = 'SAN_SANG_BAN'");

      assertEquals(
          tongMay, tongTon, "RB37: tong soLuongTon phai bang tong so may SAN_SANG_BAN");
    }
  }

  private static long dem(Connection conn, String sql) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      return rs.getLong(1);
    }
  }
}
