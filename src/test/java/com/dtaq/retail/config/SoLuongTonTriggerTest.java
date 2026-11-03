package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử trigger V6 bảo vệ bất biến tồn kho RB37 trên cơ sở dữ liệu phát triển.
 *
 * <p>RB37: {@code DongSanPham.soLuongTon} phải bằng số bản ghi {@code SanPham} của dòng đó đang ở
 * trạng thái {@code SAN_SANG_BAN}. Mỗi bài kiểm thử mở một giao dịch riêng và luôn rollback, nên dữ
 * liệu thật không bị thay đổi.
 */
class SoLuongTonTriggerTest {

  /** Dòng sản phẩm có sẵn cả máy SAN_SANG_BAN và máy DA_BAN trong dữ liệu mẫu. */
  private static final String DONG_CO_SAN = "DSP-2026-000018";

  /** Dòng sản phẩm dùng cho các bài kiểm thử thêm/xóa máy. */
  private static final String DONG_THEM_MAY = "DSP-2026-000001";

  private static final String MIGRATION =
      "db/migration/V6__trigger_so_luong_ton.sql";

  private Connection ketNoi;

  @BeforeAll
  static void kiemTraCsdl() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
  }

  @BeforeEach
  void moGiaoDich() throws SQLException, IOException {
    ketNoi = ConnectDB.getDataSource().getConnection();
    ketNoi.setAutoCommit(false);
    damBaoTrigger(ketNoi);
  }

  @AfterEach
  void hoanNguyen() throws SQLException {
    if (ketNoi == null) {
      return;
    }
    try {
      ketNoi.rollback();
    } finally {
      ketNoi.close();
      ketNoi = null;
    }
  }

  @Test
  @DisplayName("Chuyển máy SAN_SANG_BAN sang DA_BAN thì tồn giảm 1")
  void chuyenMaySangDaBanThiTonGiamMot() throws SQLException {
    String serial = serialTheoTrangThai(DONG_CO_SAN, "SAN_SANG_BAN");
    assertNotNull(serial, "khong co may SAN_SANG_BAN trong dong " + DONG_CO_SAN);
    int tonTruoc = tonCuaDong(DONG_CO_SAN);

    capNhatTrangThai(serial, "DA_BAN");

    assertEquals(
        tonTruoc - 1,
        tonCuaDong(DONG_CO_SAN),
        "RB37: soLuongTon phai giam 1 khi mot may roi khoi SAN_SANG_BAN");
    assertEquals(
        soMaySanSangBan(DONG_CO_SAN),
        tonCuaDong(DONG_CO_SAN),
        "RB37: soLuongTon phai bang so may SAN_SANG_BAN");
  }

  @Test
  @DisplayName("Chuyển máy DA_BAN trở lại SAN_SANG_BAN thì tồn tăng 1")
  void chuyenMayVeSanSangBanThiTonTangMot() throws SQLException {
    String serial = serialTheoTrangThai(DONG_CO_SAN, "DA_BAN");
    assertNotNull(serial, "khong co may DA_BAN trong dong " + DONG_CO_SAN);
    int tonTruoc = tonCuaDong(DONG_CO_SAN);

    capNhatTrangThai(serial, "SAN_SANG_BAN");

    assertEquals(
        tonTruoc + 1,
        tonCuaDong(DONG_CO_SAN),
        "RB37: soLuongTon phai tang 1 khi mot may quay lai SAN_SANG_BAN");
    assertEquals(
        soMaySanSangBan(DONG_CO_SAN),
        tonCuaDong(DONG_CO_SAN),
        "RB37: soLuongTon phai bang so may SAN_SANG_BAN");
  }

  @Test
  @DisplayName("Thêm máy mới cập nhật tồn theo trạng thái")
  void themMayMoiCapNhatTonTheoTrangThai() throws SQLException {
    int tonTruoc = tonCuaDong(DONG_THEM_MAY);

    themMay("TEST-TRG-0001", DONG_THEM_MAY, "SAN_SANG_BAN");
    assertEquals(
        tonTruoc + 1,
        tonCuaDong(DONG_THEM_MAY),
        "RB37: them may SAN_SANG_BAN phai lam soLuongTon tang 1");

    int tonSauKhiThemMayBanDuoc = tonCuaDong(DONG_THEM_MAY);
    themMay("TEST-TRG-0002", DONG_THEM_MAY, "HANG_LOI");
    assertEquals(
        tonSauKhiThemMayBanDuoc,
        tonCuaDong(DONG_THEM_MAY),
        "RB37: them may HANG_LOI khong duoc lam thay doi soLuongTon");
    assertEquals(
        soMaySanSangBan(DONG_THEM_MAY),
        tonCuaDong(DONG_THEM_MAY),
        "RB37: soLuongTon phai bang so may SAN_SANG_BAN");
  }

  @Test
  @DisplayName("Xóa máy SAN_SANG_BAN thì tồn giảm 1")
  void xoaMaySanSangBanThiTonGiamMot() throws SQLException {
    String serial = serialXoaDuoc(DONG_CO_SAN);
    assertNotNull(serial, "khong co may SAN_SANG_BAN nao xoa duoc trong dong " + DONG_CO_SAN);
    int tonTruoc = tonCuaDong(DONG_CO_SAN);

    try (PreparedStatement ps =
        ketNoi.prepareStatement("DELETE FROM \"SanPham\" WHERE \"soSerial\" = ?")) {
      ps.setString(1, serial);
      assertEquals(1, ps.executeUpdate(), "phai xoa dung mot may");
    }

    assertEquals(
        tonTruoc - 1,
        tonCuaDong(DONG_CO_SAN),
        "RB37: soLuongTon phai giam 1 khi xoa may SAN_SANG_BAN");
    assertEquals(
        soMaySanSangBan(DONG_CO_SAN),
        tonCuaDong(DONG_CO_SAN),
        "RB37: soLuongTon phai bang so may SAN_SANG_BAN");
  }

  private int tonCuaDong(String maDongSP) throws SQLException {
    String sql = "SELECT \"soLuongTon\" FROM \"DongSanPham\" WHERE \"maDongSP\" = ?";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maDongSP);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "khong tim thay dong san pham " + maDongSP);
        return rs.getInt(1);
      }
    }
  }

  private int soMaySanSangBan(String maDongSP) throws SQLException {
    String sql =
        "SELECT count(*) FROM \"SanPham\" "
            + "WHERE \"maDongSP\" = ? AND \"trangThai\" = 'SAN_SANG_BAN'";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maDongSP);
      try (ResultSet rs = ps.executeQuery()) {
        assertTrue(rs.next(), "khong dem duoc so may cua dong " + maDongSP);
        return rs.getInt(1);
      }
    }
  }

  private String serialTheoTrangThai(String maDongSP, String trangThai) throws SQLException {
    String sql =
        "SELECT \"soSerial\" FROM \"SanPham\" "
            + "WHERE \"maDongSP\" = ? AND \"trangThai\" = ? ORDER BY \"soSerial\" LIMIT 1";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maDongSP);
      ps.setString(2, trangThai);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    }
  }

  /**
   * Chọn máy SAN_SANG_BAN của dòng chưa được tham chiếu bởi hóa đơn hay phiếu đổi/trả.
   *
   * <p>Các bảng {@code ChiTietHoaDon}, {@code ChiTietPhieuDoi}, {@code SanPhamLoi} đều dùng khóa
   * ngoại ON DELETE RESTRICT, nên bài kiểm thử xóa chỉ có thể xóa máy chưa bị tham chiếu.
   */
  private String serialXoaDuoc(String maDongSP) throws SQLException {
    String sql =
        "SELECT s.\"soSerial\" FROM \"SanPham\" s "
            + "WHERE s.\"maDongSP\" = ? AND s.\"trangThai\" = 'SAN_SANG_BAN' "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM \"ChiTietHoaDon\" c "
            + "WHERE c.\"soSerial\" = s.\"soSerial\" AND c.\"maDongSP\" = s.\"maDongSP\") "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM \"ChiTietPhieuDoi\" p WHERE p.\"chiTietSanPhamDoi\" = s.\"soSerial\") "
            + "AND NOT EXISTS ("
            + "SELECT 1 FROM \"SanPhamLoi\" l WHERE l.\"soSerial\" = s.\"soSerial\") "
            + "ORDER BY s.\"soSerial\" LIMIT 1";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, maDongSP);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() ? rs.getString(1) : null;
      }
    }
  }

  private void capNhatTrangThai(String soSerial, String trangThai) throws SQLException {
    try (PreparedStatement ps =
        ketNoi.prepareStatement("UPDATE \"SanPham\" SET \"trangThai\" = ? WHERE \"soSerial\" = ?")) {
      ps.setString(1, trangThai);
      ps.setString(2, soSerial);
      assertEquals(1, ps.executeUpdate(), "phai cap nhat dung mot may");
    }
  }

  private void themMay(String soSerial, String maDongSP, String trangThai) throws SQLException {
    String maNCC = maNhaCungCapDauTien();
    String sql =
        "INSERT INTO \"SanPham\" (\"soSerial\", \"maNCC\", \"maDongSP\", \"giaNhap\", "
            + "\"ngayNhapKhoBanDau\", \"trangThai\", \"soLanDoiTra\", \"soLanSua\") "
            + "VALUES (?, ?, ?, ?, CURRENT_DATE, ?, 0, 0)";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql)) {
      ps.setString(1, soSerial);
      ps.setString(2, maNCC);
      ps.setString(3, maDongSP);
      ps.setBigDecimal(4, new BigDecimal("1000000"));
      ps.setString(5, trangThai);
      assertEquals(1, ps.executeUpdate(), "phai them dung mot may");
    }
  }

  private String maNhaCungCapDauTien() throws SQLException {
    String sql = "SELECT \"maNCC\" FROM \"NhaCungCap\" ORDER BY \"maNCC\" LIMIT 1";
    try (PreparedStatement ps = ketNoi.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      assertTrue(rs.next(), "khong co nha cung cap nao de gan cho may moi");
      return rs.getString(1);
    }
  }

  /**
   * Bảo đảm trigger V6 có mặt trong giao dịch hiện tại.
   *
   * <p>Cơ sở dữ liệu phát triển có thể mới ở phiên bản V5 (Flyway chỉ chạy khi khởi động ứng dụng),
   * nên nếu trigger chưa được cài đặt thì lớp kiểm thử nạp chính nội dung của file migration V6
   * trong giao dịch đang mở; rollback ở {@link #hoanNguyen()} sẽ hoàn nguyên mọi thay đổi.
   */
  private void damBaoTrigger(Connection conn) throws SQLException, IOException {
    if (triggerDaTonTai(conn)) {
      return;
    }
    for (String lenh : docCacCauLenh()) {
      try (Statement st = conn.createStatement()) {
        st.execute(lenh);
      }
    }
  }

  private boolean triggerDaTonTai(Connection conn) throws SQLException {
    String sql = "SELECT 1 FROM pg_trigger WHERE tgname = 'trg_sanpham_cap_nhat_ton'";
    try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      return rs.next();
    }
  }

  /** Đọc file migration V6 và tách thành từng câu lệnh, bỏ qua dấu ; bên trong chuỗi và $$. */
  private List<String> docCacCauLenh() throws IOException {
    String script;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(MIGRATION)) {
      assertNotNull(in, "khong doc duoc file migration " + MIGRATION);
      script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    List<String> ketQua = new ArrayList<>();
    StringBuilder hienTai = new StringBuilder();
    int i = 0;
    while (i < script.length()) {
      char c = script.charAt(i);
      if (c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
        int hetDong = script.indexOf('\n', i);
        i = hetDong < 0 ? script.length() : hetDong + 1;
        hienTai.append('\n');
      } else if (c == '\'') {
        int j = i + 1;
        while (j < script.length()) {
          if (script.charAt(j) == '\'') {
            if (j + 1 < script.length() && script.charAt(j + 1) == '\'') {
              j += 2;
              continue;
            }
            j++;
            break;
          }
          j++;
        }
        hienTai.append(script, i, j);
        i = j;
      } else if (c == '$' && laMoDollarQuote(script, i)) {
        String the = script.substring(i, script.indexOf('$', i + 1) + 1);
        int het = script.indexOf(the, i + the.length());
        int cuoi = het < 0 ? script.length() : het + the.length();
        hienTai.append(script, i, cuoi);
        i = cuoi;
      } else if (c == ';') {
        if (!hienTai.toString().isBlank()) {
          ketQua.add(hienTai.toString());
        }
        hienTai.setLength(0);
        i++;
      } else {
        hienTai.append(c);
        i++;
      }
    }
    if (!hienTai.toString().isBlank()) {
      ketQua.add(hienTai.toString());
    }
    return ketQua;
  }

  private boolean laMoDollarQuote(String script, int viTri) {
    int j = viTri + 1;
    while (j < script.length()
        && (Character.isLetterOrDigit(script.charAt(j)) || script.charAt(j) == '_')) {
      j++;
    }
    return j < script.length() && script.charAt(j) == '$';
  }
}
