package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.*;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.impl.DanhMucSanPhamDaoImpl;
import com.dtaq.retail.dao.impl.DongSanPhamImpl;
import com.dtaq.retail.dao.impl.KhuyenMaiDaoImpl;
import com.dtaq.retail.dao.impl.ThongKeSanPhamDaoImpl;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Chỉ chạy khi chủ động trỏ tới cluster test W6 đã xác minh. */
class ProductInsightsDbTest {
  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 12, 31);
  private DataSource db;
  private ThongKeSanPhamController reports;

  @BeforeEach
  void setUp() throws Exception {
    Assumptions.assumeTrue("jdbc:postgresql://127.0.0.1:55476/dtaq_w6_test"
        .equals(System.getenv("DTAQ_DB_URL")), "Cần chủ động chọn DB test W6");
    db = ConnectDB.getDataSource();
    try (var conn = db.getConnection(); var stmt = conn.createStatement();
        var rs = stmt.executeQuery("SELECT current_database(), inet_server_port()")) {
      assertTrue(rs.next());
      assertEquals("dtaq_w6_test", rs.getString(1));
      assertEquals(55476, rs.getInt(2));
    }
    CurrentSession.set(new NhanVien("NV-TEST", "Quản lý test", "0900000000", null,
        FROM, "test", "hash", LoaiQuyen.NHAN_VIEN_QUAN_LY, true));
    reports = new ThongKeSanPhamController(new ThongKeSanPhamDaoImpl(db));
  }

  @AfterEach
  void clearSession() {
    CurrentSession.clear();
  }

  @Test
  void nhomNhanXetKhopTruyVanDocLapTrenDb() throws Exception {
    var result = reports.xemBaoCao(FROM, TO, null);
    assertTrue(result.thanhCong(), result.thongBao());
    assertFalse(result.duLieu().chiTiet().isEmpty());
    String sql = """
        WITH facts AS (
          SELECT dsp."soLuongTon" AS ton,
            EXISTS(SELECT 1 FROM "ChiTietHoaDon" ct JOIN "HoaDon" hd ON hd."maHD"=ct."maHD"
              WHERE ct."maDongSP"=dsp."maDongSP" AND hd."trangThaiThanhToan"=TRUE
                AND hd."ngayLap" BETWEEN ? AND ?) AS sold,
            EXISTS(SELECT 1 FROM "ChiTietPhieuTra" cpt
              JOIN "PhieuTra" pt ON pt."maPhieuTra"=cpt."maPhieuTra"
              JOIN "ChiTietHoaDon" ct ON ct."maHD"=cpt."maHD" AND ct."soSerial"=cpt."soSerial"
              WHERE ct."maDongSP"=dsp."maDongSP" AND pt."trangThaiXuLyPhieu"='HOAN_TAT'
                AND pt."ngayHoanTat" BETWEEN ? AND ?) AS returned
          FROM "DongSanPham" dsp)
        SELECT COUNT(*) FILTER(WHERE returned), COUNT(*) FILTER(WHERE returned AND NOT sold),
          COUNT(*) FILTER(WHERE ton>0 AND NOT sold) FROM facts
        """;
    try (var conn = db.getConnection(); var stmt = conn.prepareStatement(sql)) {
      stmt.setObject(1, FROM);
      stmt.setObject(2, TO);
      stmt.setObject(3, FROM);
      stmt.setObject(4, TO);
      try (var rs = stmt.executeQuery()) {
        assertTrue(rs.next());
        var statements = ThongKeSanPhamController.nhanXet(result.duLieu().chiTiet());
        assertTrue(statements.contains(rs.getLong(1) + " dòng có hàng trả hoàn tất trong kỳ, trong đó "
            + rs.getLong(2) + " dòng chỉ phát sinh hàng trả."));
        assertTrue(statements.contains(rs.getLong(3) + " dòng hiện còn tồn kho nhưng chưa bán trong kỳ đã chọn."));
      }
    }
  }

  @Test
  void xuatExcelVaMoLaiKhopTatCaDongDb(@TempDir Path folder) throws Exception {
    var result = reports.xemBaoCao(FROM, TO, null);
    assertTrue(result.thanhCong(), result.thongBao());
    var rows = result.duLieu().chiTiet();
    assertFalse(rows.isEmpty());
    var file = folder.resolve("doanh-so-san-pham.xlsx").toFile();
    var exported = reports.xuatExcel(file, rows);
    assertTrue(exported.thanhCong(), exported.thongBao());
    try (var book = new XSSFWorkbook(file)) {
      var sheet = book.getSheetAt(0);
      assertEquals(rows.size() + 1, sheet.getLastRowNum());
      for (int i = 0; i < rows.size(); i++) {
        var actual = sheet.getRow(i + 2);
        var expected = rows.get(i);
        assertEquals(expected.maDongSP(), actual.getCell(0).getStringCellValue());
        assertEquals(expected.tenDongSP(), actual.getCell(1).getStringCellValue());
        assertEquals(expected.tenDanhMuc(), actual.getCell(2).getStringCellValue());
        assertEquals((double) expected.soLuongBan(), actual.getCell(3).getNumericCellValue());
        assertEquals(expected.tienTruocThue().doubleValue(), actual.getCell(4).getNumericCellValue());
        assertEquals((double) expected.soLuongTra(), actual.getCell(5).getNumericCellValue());
        assertEquals(expected.doanhSoSauDieuChinh().doubleValue(), actual.getCell(6).getNumericCellValue());
      }
    }
  }

  @Test
  void previewXacNhanLuuVaNapLaiKhuyenMaiDaDanhMuc() throws Exception {
    var dao = new KhuyenMaiDaoImpl(db);
    var controller = new KhuyenMaiController(dao, new DongSanPhamImpl(db), new DanhMucSanPhamDaoImpl(db), db);
    var categories = controller.layDanhSachDanhMuc().stream().limit(2).map(d -> d.maDanhMuc()).toList();
    assertEquals(2, categories.size());
    var preview = controller.xemTruocDongApDung(null, categories, null);
    assertTrue(preview.thanhCong(), preview.thongBao());
    var codes = preview.duLieu().stream().map(d -> d.dong().maDongSP()).toList();
    assertFalse(codes.isEmpty());
    String name = "TEST Insights " + UUID.randomUUID();
    LocalDate start = LocalDate.now().plusDays(90);
    long before = dao.findAll().size();
    var stale = controller.themMoiTheoDieuKien(name, "Test DB", BigDecimal.TEN, start, start.plusDays(10),
        null, categories, null, false, List.of("DSP-STALE"));
    assertFalse(stale.thanhCong());
    assertEquals(before, dao.findAll().size());
    String createdId = null;
    try {
      var saved = controller.themMoiTheoDieuKien(name, "Test DB", BigDecimal.TEN, start, start.plusDays(10),
          null, categories, null, false, codes);
      assertTrue(saved.thanhCong(), saved.thongBao());
      createdId = saved.duLieu().maKM();
      var loaded = dao.findById(createdId).orElseThrow();
      assertEquals(name, loaded.tenKM());
      assertFalse(loaded.dangApDung());
      assertEquals(codes.stream().sorted().toList(), loaded.chiTiet().stream()
          .map(c -> c.dongSanPham().maDongSP()).sorted().toList());
    } finally {
      if (createdId != null) {
        try (var conn = db.getConnection(); var stmt = conn.prepareStatement(
            "DELETE FROM \"KhuyenMai\" WHERE \"maKM\"=? AND \"tenKM\"=?")) {
          stmt.setString(1, createdId);
          stmt.setString(2, name);
          assertEquals(1, stmt.executeUpdate());
        }
      }
    }
    assertEquals(before, dao.findAll().size());
  }
}
