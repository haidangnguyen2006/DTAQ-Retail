package com.dtaq.retail.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dtaq.retail.config.ConnectDB;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử tích hợp {@link MaGenerator} trên cơ sở dữ liệu phát triển.
 *
 * <p>Số thứ tự mong đợi được đọc từ chính cơ sở dữ liệu tại thời điểm chạy thay vì viết cứng, để bộ
 * kiểm thử không phụ thuộc vào lượng dữ liệu mẫu đang có trong cơ sở dữ liệu phát triển.
 */
class MaGeneratorTest {

  private static final int NAM = 2026;
  private static final int NAM_CHUA_CO_DU_LIEU = 2030;

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
  @DisplayName("Năm chưa có dữ liệu thì mã nối tiếp số lớn nhất hiện có, mặc định bắt đầu từ 000001")
  void namChuaCoDuLieuBatDauTuMot() throws SQLException {
    int max = maxSoHienTai("HoaDon", "maHD", "HD", NAM_CHUA_CO_DU_LIEU);

    assertEquals(
        maMongDoi("HD", NAM_CHUA_CO_DU_LIEU, max + 1),
        MaGenerator.next(ketNoi, "HoaDon", "maHD", "HD", NAM_CHUA_CO_DU_LIEU));
  }

  @Test
  @DisplayName("Năm đã có dữ liệu thì mã nối tiếp số lớn nhất đang có trong bảng")
  void noiTiepSoLonNhatTrongNam() throws SQLException {
    int max = maxSoHienTai("HoaDon", "maHD", "HD", NAM);

    assertEquals(
        maMongDoi("HD", NAM, max + 1),
        MaGenerator.next(ketNoi, "HoaDon", "maHD", "HD", NAM));
  }

  @Test
  @DisplayName("Mã tăng dần trong cùng một giao dịch và quay lui không để lại dữ liệu")
  void tangDanTrongCungGiaoDich() throws SQLException {
    int maxTruocGiaoDich = maxSoHienTai("KhachHang", "maKH", "KH", NAM);
    ketNoi.setAutoCommit(false);
    try {
      String id1 = MaGenerator.next(ketNoi, "KhachHang", "maKH", "KH", NAM);
      assertEquals(maMongDoi("KH", NAM, maxTruocGiaoDich + 1), id1);

      try (PreparedStatement statement = ketNoi.prepareStatement(
          "INSERT INTO \"KhachHang\" (\"maKH\", \"hoTen\", \"soDienThoai\") VALUES (?, ?, ?)")) {
        statement.setString(1, id1);
        statement.setString(2, "Kiểm thử");
        statement.setString(3, "0999999999");
        statement.executeUpdate();
      }

      String id2 = MaGenerator.next(ketNoi, "KhachHang", "maKH", "KH", NAM);
      assertEquals(maMongDoi("KH", NAM, soThuTu(id1) + 1), id2);
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

  /**
   * Đọc số thứ tự lớn nhất hiện có của một tiền tố trong một năm, đúng cách {@link MaGenerator} đọc.
   *
   * <p>Tên bảng và tên cột là hằng số do bài kiểm thử cung cấp, không phải dữ liệu người dùng nhập.
   *
   * @param bang tên bảng chứa cột mã
   * @param cotMa tên cột mã
   * @param tienTo tiền tố mã
   * @param nam năm cần đọc
   * @return số thứ tự lớn nhất, hoặc {@code 0} nếu chưa có mã nào
   * @throws SQLException nếu truy vấn thất bại
   */
  private int maxSoHienTai(String bang, String cotMa, String tienTo, int nam)
      throws SQLException {
    String goc = tienTo + "-" + nam + "-";
    String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(\"" + cotMa + "\" FROM ?) AS INTEGER)), 0)"
        + " FROM \"" + bang + "\" WHERE \"" + cotMa + "\" LIKE ?";
    try (PreparedStatement statement = ketNoi.prepareStatement(sql)) {
      statement.setInt(1, goc.length() + 1);
      statement.setString(2, goc + "%");
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  /** Ghép mã mong đợi từ tiền tố, năm và số thứ tự thay vì viết cứng mã đầy đủ. */
  private static String maMongDoi(String tienTo, int nam, int soThuTu) {
    return String.format("%s-%d-%06d", tienTo, nam, soThuTu);
  }

  /** Lấy phần số thứ tự ở cuối một mã đã sinh. */
  private static int soThuTu(String ma) {
    return Integer.parseInt(ma.substring(ma.lastIndexOf('-') + 1));
  }
}
