package com.dtaq.retail.dao.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.ThongKeSanPhamDao.DongThongKe;
import com.dtaq.retail.dao.ThongKeSanPhamDao.GiaoDichDoiChieu;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DAO thống kê doanh số theo sản phẩm (UC015)")
class ThongKeSanPhamDaoImplTest {

  private static final LocalDate TU = LocalDate.of(2026, 1, 1);
  private static final LocalDate DEN = LocalDate.of(2026, 1, 31);
  /** Seed V4: SAM-TV-0001 bán trên HD-2026-000001 (05/01), trả hoàn tất PT-2026-000001 (22/01). */
  private static final String MA_DONG = "DSP-2026-000001";
  private static final String MA_PHIEU_THU = "PT-TEST-UC015-HUY";

  private ThongKeSanPhamDaoImpl dao;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
    dao = new ThongKeSanPhamDaoImpl(ConnectDB.getDataSource());
  }

  @Test
  @DisplayName("Máy có thêm một phiếu trả đã hủy: doanh số và số lượng không bị nhân đôi")
  void phieuHuyKhongNhanDoiDoanhSo() throws Exception {
    List<DongThongKe> truoc = dao.thongKe(TU, DEN, MA_DONG);
    Assumptions.assumeTrue(truoc.size() == 1 && truoc.get(0).soLuongTra() == 1,
        "seed V4 chưa được áp dụng");

    try {
      chay("INSERT INTO \"PhieuTra\" (\"maPhieuTra\", \"nhanVienLap\", \"hoaDon\", \"ngayTao\","
          + " \"trangThaiDuyet\", \"trangThaiXuLyPhieu\", \"tienKhachDua\")"
          + " SELECT ?, \"nhanVienLap\", \"maHD\", DATE '2026-01-10', 'KHONG_CAN_DUYET', 'HUY', 0"
          + " FROM \"HoaDon\" WHERE \"maHD\" = 'HD-2026-000001'");
      chay("INSERT INTO \"ChiTietPhieuTra\" (\"maPhieuTra\", \"maHD\", \"soSerial\","
          + " \"tinhTrangHangTra\", \"lyDoTra\")"
          + " VALUES (?, 'HD-2026-000001', 'SAM-TV-0001', 'HANG_LOI', 'Phiếu thử đã hủy')");

      DongThongKe sau = dao.thongKe(TU, DEN, MA_DONG).get(0);
      DongThongKe goc = truoc.get(0);
      assertEquals(goc.soLuongBan(), sau.soLuongBan());
      assertEquals(0, goc.tienTruocThue().compareTo(sau.tienTruocThue()));
      assertEquals(goc.soLuongTra(), sau.soLuongTra());
      assertEquals(0, goc.doanhSoSauDieuChinh().compareTo(sau.doanhSoSauDieuChinh()));
    } finally {
      chay("DELETE FROM \"ChiTietPhieuTra\" WHERE \"maPhieuTra\" = ?");
      chay("DELETE FROM \"PhieuTra\" WHERE \"maPhieuTra\" = ?");
    }
  }

  @Test
  @DisplayName("Đối chiếu (9.a) khớp số liệu tổng hợp của mọi dòng sản phẩm trong năm")
  void doiChieuKhopTongHop() {
    LocalDate tu = LocalDate.of(2026, 1, 1);
    LocalDate den = LocalDate.of(2026, 12, 31);
    List<DongThongKe> dongs = dao.thongKe(tu, den, null);
    Assumptions.assumeTrue(dongs.stream().anyMatch(d -> d.soLuongTra() > 0), "seed V4 chưa được áp dụng");

    for (DongThongKe dong : dongs) {
      List<GiaoDichDoiChieu> gds = dao.doiChieu(tu, den, dong.maDongSP());
      assertEquals(dong.soLuongBan(), gds.stream().filter(g -> !g.laTra()).count(), dong.maDongSP());
      assertEquals(dong.soLuongTra(), gds.stream().filter(GiaoDichDoiChieu::laTra).count(), dong.maDongSP());
      BigDecimal tong = gds.stream().map(GiaoDichDoiChieu::giaTri).reduce(BigDecimal.ZERO, BigDecimal::add);
      assertEquals(0, dong.doanhSoSauDieuChinh().compareTo(tong), dong.maDongSP());
    }
  }

  private static void chay(String sql) throws Exception {
    try (Connection c = ConnectDB.getDataSource().getConnection();
        PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setString(1, MA_PHIEU_THU);
      ps.executeUpdate();
    }
  }
}
