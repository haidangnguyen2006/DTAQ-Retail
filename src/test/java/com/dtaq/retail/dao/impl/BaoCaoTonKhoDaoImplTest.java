package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.BaoCaoTonKhoDao.DongTonLau;
import com.dtaq.retail.entity.TrangThaiSanPham;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử tích hợp báo cáo tồn kho lâu ngày trên PostgreSQL. */
@DisplayName("Báo cáo tồn kho lâu ngày (UC014)")
class BaoCaoTonKhoDaoImplTest {

  private BaoCaoTonKhoDaoImpl dao;
  private String maNCC;
  private String maDongSP;
  private LocalDate ngayHienTaiCuaDb;
  private String serialPrefix;
  private int serialSequence;
  private final List<String> serialsForCleanup = new ArrayList<>();

  @BeforeEach
  void setUp() throws SQLException {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "CSDL PostgreSQL không khả dụng");
    dao = new BaoCaoTonKhoDaoImpl(ConnectDB.getDataSource());
    serialPrefix = "UT-TON-" + UUID.randomUUID();
    serialSequence = 0;
    serialsForCleanup.clear();
    try (Connection connection = ConnectDB.getDataSource().getConnection();
        var statement = connection.createStatement();
        var resultSet = statement.executeQuery(
            "SELECT CURRENT_DATE, ncc.\"maNCC\", dsp.\"maDongSP\""
                + " FROM \"NhaCungCap\" ncc CROSS JOIN \"DongSanPham\" dsp"
                + " ORDER BY ncc.\"maNCC\", dsp.\"maDongSP\" LIMIT 1")) {
      assertTrue(resultSet.next(), "Database cần có ít nhất một nhà cung cấp và dòng sản phẩm seed");
      ngayHienTaiCuaDb = resultSet.getObject(1, LocalDate.class);
      maNCC = resultSet.getString(2);
      maDongSP = resultSet.getString(3);
    }
  }

  @AfterEach
  void tearDown() throws SQLException {
    if (serialsForCleanup.isEmpty() || !ConnectDB.isAvailable()) {
      return;
    }
    try (Connection connection = ConnectDB.getDataSource().getConnection();
        var statement = connection.prepareStatement("DELETE FROM \"SanPham\" WHERE \"soSerial\" = ?")) {
      for (String serial : serialsForCleanup) {
        statement.setString(1, serial);
        statement.executeUpdate();
      }
    }
  }

  @Test
  @DisplayName("Lấy đúng các trạng thái còn tồn và bao gồm máy đúng ngưỡng ngày")
  void findTonLau_locTrangThaiTonVaGomBienNguong() throws SQLException {
    int nguongNgay = 30;
    String readyAtBoundary = insertSanPham(TrangThaiSanPham.SAN_SANG_BAN, nguongNgay);
    String readyOlder = insertSanPham(TrangThaiSanPham.SAN_SANG_BAN, nguongNgay + 1L);
    String defective = insertSanPham(TrangThaiSanPham.HANG_LOI, nguongNgay + 5L);
    String warranty = insertSanPham(TrangThaiSanPham.DANG_BAO_HANH, nguongNgay + 7L);
    insertSanPham(TrangThaiSanPham.SAN_SANG_BAN, nguongNgay - 1L);
    insertSanPham(TrangThaiSanPham.DA_BAN, nguongNgay + 10L);
    insertSanPham(TrangThaiSanPham.DA_TRA_NHA_CUNG_CAP, nguongNgay + 10L);
    insertSanPham(TrangThaiSanPham.DA_THANH_LY, nguongNgay + 10L);

    List<DongTonLau> rows = dao.findTonLau(nguongNgay, false);

    assertEquals(Set.of(readyAtBoundary, readyOlder, defective, warranty),
        serials(rows, serialPrefix), "Chỉ các trạng thái còn tồn trong fixture phải được trả về");
    DongTonLau boundary = rows.stream()
        .filter(row -> row.soSerial().equals(readyAtBoundary)).findFirst().orElseThrow();
    assertEquals(nguongNgay, boundary.soNgayTon(), "Số ngày bằng ngưỡng phải được lấy (>=)");
    assertEquals(ngayHienTaiCuaDb.minusDays(nguongNgay), boundary.ngayNhapKhoBanDau());
    assertEquals(maDongSP, boundary.maDongSP());
    assertTrue(!boundary.tenDongSP().isBlank());
    assertTrue(!boundary.tenDanhMuc().isBlank());
    assertEquals(TrangThaiSanPham.SAN_SANG_BAN, boundary.trangThai());
  }

  @Test
  @DisplayName("Checkbox chỉ sẵn sàng bán loại máy lỗi và đang bảo hành")
  void findTonLau_chiLaySanSangBan() throws SQLException {
    String ready = insertSanPham(TrangThaiSanPham.SAN_SANG_BAN, 60);
    insertSanPham(TrangThaiSanPham.HANG_LOI, 60);
    insertSanPham(TrangThaiSanPham.DANG_BAO_HANH, 60);

    List<DongTonLau> rows = dao.findTonLau(60, true);

    assertEquals(Set.of(ready), serials(rows, serialPrefix));
    assertTrue(rows.stream().filter(row -> row.soSerial().startsWith(serialPrefix))
        .allMatch(row -> row.trangThai() == TrangThaiSanPham.SAN_SANG_BAN));
  }

  @Test
  @DisplayName("Không có máy nào thì trả danh sách rỗng")
  void findTonLau_khongCoKetQua() {
    assertTrue(dao.findTonLau(Integer.MAX_VALUE, false).isEmpty());
  }

  @Test
  @DisplayName("Từ chối ngưỡng ngày không dương")
  void findTonLau_nguongKhongHopLe() {
    assertThrows(IllegalArgumentException.class, () -> dao.findTonLau(0, false));
    assertThrows(IllegalArgumentException.class, () -> dao.findTonLau(-1, true));
  }

  private String insertSanPham(TrangThaiSanPham status, long ageInDays) throws SQLException {
    String serial = serialPrefix + "-" + (++serialSequence);
    try (Connection connection = ConnectDB.getDataSource().getConnection();
        var statement = connection.prepareStatement(
            "INSERT INTO \"SanPham\""
                + " (\"soSerial\", \"maNCC\", \"maDongSP\", \"giaNhap\","
                + " \"ngayNhapKhoBanDau\", \"trangThai\", \"soLanDoiTra\", \"soLanSua\")"
                + " VALUES (?, ?, ?, 0, ?, ?, 0, 0)")) {
      statement.setString(1, serial);
      statement.setString(2, maNCC);
      statement.setString(3, maDongSP);
      statement.setDate(4, Date.valueOf(ngayHienTaiCuaDb.minusDays(ageInDays)));
      statement.setString(5, status.name());
      statement.executeUpdate();
    }
    serialsForCleanup.add(serial);
    return serial;
  }

  private static Set<String> serials(List<DongTonLau> rows, String prefix) {
    return rows.stream().map(DongTonLau::soSerial)
        .filter(serial -> serial.startsWith(prefix))
        .collect(java.util.stream.Collectors.toSet());
  }
}
