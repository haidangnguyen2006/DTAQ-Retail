package com.dtaq.retail.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử schema khuyến mãi sau V15 và V16.
 *
 * <p>V15 tạo bảng chi tiết và backfill. V16 bỏ cột liên kết cũ. Các migration còn thiếu được chạy
 * trong giao dịch của bài kiểm thử và rollback sau đó.
 */
class ChiTietKhuyenMaiMigrationTest {

  private static final String V15 = "db/migration/V15__chi_tiet_khuyen_mai.sql";
  private static final String V16 = "db/migration/V16__bo_khuyenmai_madongsp.sql";

  private Connection ketNoi;

  @BeforeAll
  static void kiemTraCsdl() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
  }

  @BeforeEach
  void moGiaoDich() throws SQLException, IOException {
    ketNoi = ConnectDB.getDataSource().getConnection();
    ketNoi.setAutoCommit(false);
    damBaoMigration(ketNoi);
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
  @DisplayName("V15 tạo bảng ChiTietKhuyenMai")
  void bangChiTietKhuyenMaiTonTai() throws SQLException {
    assertTrue(bangTonTai(ketNoi, "ChiTietKhuyenMai"),
        "V15 phải tạo bảng \"ChiTietKhuyenMai\"");
  }

  @Test
  @DisplayName("V16 xóa cột KhuyenMai.maDongSP")
  void v16XoaCotMaDongSPCuaKhuyenMai() throws SQLException {
    assertFalse(cotMaDongSPCuaKhuyenMaiTonTai(ketNoi), "V16 phải xóa cột KhuyenMai.maDongSP");
  }

  private boolean cotMaDongSPCuaKhuyenMaiTonTai(Connection conn) throws SQLException {
    String sql =
        "SELECT 1 FROM information_schema.columns "
            + "WHERE table_schema = current_schema() "
            + "AND table_name = 'KhuyenMai' AND column_name = 'maDongSP'";
    try (Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      return rs.next();
    }
  }

  @Test
  @DisplayName("Khóa chính của ChiTietKhuyenMai là (maKM, maDongSP)")
  void khoaChinhDungThietKe() throws SQLException {
    String sql =
        "SELECT kcu.column_name FROM information_schema.table_constraints tc "
            + "JOIN information_schema.key_column_usage kcu "
            + "ON tc.constraint_name = kcu.constraint_name "
            + "AND tc.constraint_schema = kcu.constraint_schema "
            + "WHERE tc.table_schema = current_schema() "
            + "AND tc.table_name = 'ChiTietKhuyenMai' "
            + "AND tc.constraint_type = 'PRIMARY KEY' "
            + "ORDER BY kcu.ordinal_position";
    try (Statement st = ketNoi.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      List<String> cot = new ArrayList<>();
      while (rs.next()) {
        cot.add(rs.getString(1));
      }
      assertEquals(List.of("maKM", "maDongSP"), cot,
          "khóa chính của \"ChiTietKhuyenMai\" phải là (maKM, maDongSP)");
    }
  }

  @Test
  @DisplayName("Khóa ngoại dùng đúng hành vi xóa CASCADE/RESTRICT")
  void khoaNgoaiDungHanhViXoa() throws SQLException {
    String sql =
        "SELECT a.attname, c.confdeltype FROM pg_constraint c "
            + "JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1] "
            + "WHERE c.conrelid = '\"ChiTietKhuyenMai\"'::regclass AND c.contype = 'f'";
    Map<String, String> hanhViTheoCot = new LinkedHashMap<>();
    try (Statement st = ketNoi.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        hanhViTheoCot.put(rs.getString(1), rs.getString(2));
      }
    }

    assertEquals("c", hanhViTheoCot.get("maKM"),
        "khóa ngoại maKM phải là ON DELETE CASCADE (c)");
    assertEquals("r", hanhViTheoCot.get("maDongSP"),
        "khóa ngoại maDongSP phải là ON DELETE RESTRICT (r)");
  }

  @Test
  @DisplayName("Có chỉ mục tra cứu (maDongSP, maKM)")
  void chiMucTraCuuTonTai() throws SQLException {
    String dinhNghia = null;
    try (Statement st = ketNoi.createStatement();
        ResultSet rs = st.executeQuery(
            "SELECT pg_get_indexdef(i.indexrelid) FROM pg_index i "
                + "WHERE i.indexrelid = to_regclass('idx_ctkm_dong_san_pham')")) {
      if (rs.next()) {
        dinhNghia = rs.getString(1);
      }
    }
    assertNotNull(dinhNghia, "V15 phải tạo chỉ mục idx_ctkm_dong_san_pham");
    String dinhNghiaChot = dinhNghia;
    int viTriDongSanPham = dinhNghiaChot.indexOf("\"maDongSP\"");
    int viTriKhuyenMai = dinhNghiaChot.indexOf("\"maKM\"");
    assertTrue(viTriDongSanPham >= 0 && viTriKhuyenMai > viTriDongSanPham,
        () -> "idx_ctkm_dong_san_pham phải có thứ tự cột (maDongSP, maKM) nhưng thấy: "
            + dinhNghiaChot);
  }

  @Test
  @DisplayName("Sao chép dữ liệu: số dòng chi tiết không ít hơn số khuyến mãi")
  void soDongChiTietKhongItHonSoKhuyenMai() throws SQLException {
    long soChiTiet = dem("SELECT count(*) FROM \"ChiTietKhuyenMai\"");
    long soKhuyenMai = dem("SELECT count(*) FROM \"KhuyenMai\"");

    assertTrue(soChiTiet >= soKhuyenMai,
        () -> "số dòng ChiTietKhuyenMai (" + soChiTiet
            + ") phải >= số dòng KhuyenMai (" + soKhuyenMai + ")");
  }

  @Test
  @DisplayName("Mỗi KhuyenMai có ít nhất một dòng chi tiết")
  void moiKhuyenMaiCoItNhatMotChiTiet() throws SQLException {
    String sql =
        "SELECT k.\"maKM\" FROM \"KhuyenMai\" k "
            + "WHERE NOT EXISTS ("
            + "SELECT 1 FROM \"ChiTietKhuyenMai\" c WHERE c.\"maKM\" = k.\"maKM\""
            + ") ORDER BY k.\"maKM\"";
    List<String> thieuChiTiet = new ArrayList<>();
    try (Statement st = ketNoi.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      while (rs.next()) {
        thieuChiTiet.add(rs.getString(1));
      }
    }
    assertTrue(thieuChiTiet.isEmpty(),
        () -> "các khuyến mãi chưa được sao chép sang ChiTietKhuyenMai: " + thieuChiTiet);
  }

  private long dem(String sql) throws SQLException {
    try (Statement st = ketNoi.createStatement();
        ResultSet rs = st.executeQuery(sql)) {
      assertTrue(rs.next(), "không đếm được dữ liệu: " + sql);
      return rs.getLong(1);
    }
  }

  private boolean bangTonTai(Connection conn, String tenBang) throws SQLException {
    String sql = "SELECT to_regclass(?) IS NOT NULL";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, '"' + tenBang + '"');
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next() && rs.getBoolean(1);
      }
    }
  }

  private void damBaoMigration(Connection conn) throws SQLException, IOException {
    if (!bangTonTai(conn, "ChiTietKhuyenMai")) {
      chayMigration(conn, V15);
    }
    if (cotMaDongSPCuaKhuyenMaiTonTai(conn)) {
      chayMigration(conn, V16);
    }
  }

  private void chayMigration(Connection conn, String migration) throws SQLException, IOException {
    for (String lenh : docCacCauLenh(migration)) {
      try (Statement st = conn.createStatement()) {
        st.execute(lenh);
      }
    }
  }

  /**
   * Đọc file migration và tách thành từng câu lệnh.
   *
   * <p>V15 và V16 chỉ có câu lệnh DDL/DML đơn giản, không chứa chuỗi có dấu {@code ;} hay khối {@code $$},
   * nên chỉ cần bỏ dòng chú thích rồi tách theo dấu chấm phẩy.
   */
  private List<String> docCacCauLenh(String migration) throws IOException {
    String script;
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(migration)) {
      assertNotNull(in, "khong doc duoc file migration " + migration);
      script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

    String khongChuThich = String.join("\n", script.lines()
        .filter(line -> !line.strip().startsWith("--"))
        .toList());

    List<String> ketQua = new ArrayList<>();
    for (String phan : khongChuThich.split(";")) {
      if (!phan.isBlank()) {
        ketQua.add(phan.strip());
      }
    }
    assertFalse(ketQua.isEmpty(), "file migration " + migration + " khong co cau lenh nao");
    return ketQua;
  }
}
