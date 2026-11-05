package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.ThongKeDoanhThuDao.DongDoanhThu;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Đối chiếu DAO UC017 với truy vấn tính độc lập trên dữ liệu mẫu. */
@DisplayName("DAO thống kê doanh thu (UC017)")
class ThongKeDoanhThuDaoImplTest {

  private static final LocalDate TU = LocalDate.of(2025, 1, 1);
  private static final LocalDate DEN = LocalDate.of(2026, 12, 31);

  private ThongKeDoanhThuDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new ThongKeDoanhThuDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("Doanh thu = tổng snapshot của hóa đơn đã thanh toán trong kỳ")
  void doanhThuKhopTinhTay() throws Exception {
    List<DongDoanhThu> dongs = dao.doanhThu(TU, DEN);
    Assumptions.assumeFalse(dongs.isEmpty(), "seed V4 chưa được áp dụng");

    BigDecimal[] mong = tong("SELECT SUM(ct.\"tienSauKhuyenMai\"), SUM(ct.\"tienThue\")"
        + " FROM \"HoaDon\" hd JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = hd.\"maHD\""
        + " WHERE hd.\"trangThaiThanhToan\" AND hd.\"ngayLap\" BETWEEN ? AND ?");
    assertEquals(0, mong[0].compareTo(cong(dongs, false)));
    assertEquals(0, mong[1].compareTo(cong(dongs, true)));
  }

  @Test
  @DisplayName("Chỉ trừ phiếu trả hoàn tất; phiếu từ chối/chờ xử lý không bị trừ")
  void chiTruPhieuHoanTat() throws Exception {
    List<DongDoanhThu> dongs = dao.dieuChinh(TU, DEN);
    Assumptions.assumeFalse(dongs.isEmpty(), "seed V4 chưa được áp dụng");

    BigDecimal[] mong = tong("SELECT -SUM(ct.\"tienSauKhuyenMai\"), -SUM(ct.\"tienThue\")"
        + " FROM \"PhieuTra\" pt JOIN \"ChiTietPhieuTra\" c ON c.\"maPhieuTra\" = pt.\"maPhieuTra\""
        + " JOIN \"ChiTietHoaDon\" ct ON ct.\"maHD\" = c.\"maHD\" AND ct.\"soSerial\" = c.\"soSerial\""
        + " WHERE pt.\"trangThaiXuLyPhieu\" = 'HOAN_TAT' AND pt.\"ngayHoanTat\" BETWEEN ? AND ?");
    assertEquals(0, mong[0].compareTo(cong(dongs, false)));
    assertEquals(0, mong[1].compareTo(cong(dongs, true)));

    Set<String> chuaHoanTat = maPhieu("SELECT \"maPhieuTra\" FROM \"PhieuTra\""
        + " WHERE \"trangThaiXuLyPhieu\" <> 'HOAN_TAT'");
    Assumptions.assumeFalse(chuaHoanTat.isEmpty(), "seed không có phiếu chưa hoàn tất");
    assertTrue(dongs.stream().noneMatch(d -> chuaHoanTat.contains(d.maPhieu())));
  }

  @Test
  @DisplayName("Kỳ đảo ngược bị từ chối")
  void kyKhongHopLe() {
    assertThrows(IllegalArgumentException.class, () -> dao.doanhThu(DEN, TU));
  }

  private static BigDecimal cong(List<DongDoanhThu> dongs, boolean thue) {
    return dongs.stream().map(d -> thue ? d.thue() : d.truocThue())
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static BigDecimal[] tong(String sql) throws Exception {
    try (Connection c = ConnectDB.getDataSource().getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setObject(1, TU);
      ps.setObject(2, DEN);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return new BigDecimal[] {zero(rs.getBigDecimal(1)), zero(rs.getBigDecimal(2))};
      }
    }
  }

  private static Set<String> maPhieu(String sql) throws Exception {
    try (Connection c = ConnectDB.getDataSource().getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      Set<String> ketQua = new HashSet<>();
      while (rs.next()) {
        ketQua.add(rs.getString(1));
      }
      return ketQua;
    }
  }

  private static BigDecimal zero(BigDecimal v) {
    return v == null ? BigDecimal.ZERO : v;
  }
}
