package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dtaq.retail.controller.ThongKeNhanVienController.BaoCao;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.ThongKeNhanVienDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.DateRange;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Thống kê doanh số theo nhân viên (UC016)")
class ThongKeNhanVienControllerTest {

  private static final DateRange KY = new DateRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
  private static final ThongKeNhanVienDao.DongThongKeNhanVien DUY = new ThongKeNhanVienDao.DongThongKeNhanVien(
      "NVBANHANG-2025-001", "Trần Văn Duy", true, 2, new BigDecimal("50000000"), new BigDecimal("32900000"));
  private static final ThongKeNhanVienDao.DongThongKeNhanVien MAI = new ThongKeNhanVienDao.DongThongKeNhanVien(
      "NVBANHANG-2025-002", "Lê Thị Mai", false, 1, new BigDecimal("20000000"), BigDecimal.ZERO);

  private final FakeThongKeDao dao = new FakeThongKeDao();
  private final ThongKeNhanVienController controller = new ThongKeNhanVienController(dao, new FakeNhanVienDao());

  @BeforeEach
  void dangNhapQuanLy() {
    dangNhap(LoaiQuyen.NHAN_VIEN_QUAN_LY);
  }

  @AfterEach
  void dangXuat() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Tổng cộng bằng tổng các dòng; sau điều chỉnh = doanh số − điều chỉnh")
  void tongCong() {
    BaoCao baoCao = controller.thongKe(KY, null);
    assertEquals(3, baoCao.tongSoHoaDon());
    assertEquals(new BigDecimal("70000000"), baoCao.tongDoanhSo());
    assertEquals(new BigDecimal("32900000"), baoCao.tongDieuChinh());
    assertEquals(new BigDecimal("37100000"), baoCao.tongSauDieuChinh());
    assertEquals(new BigDecimal("17100000"), DUY.sauDieuChinh());
  }

  @Test
  @DisplayName("Phạm vi nhân viên trống nghĩa là tất cả; có mã thì truyền xuống DAO")
  void phamViNhanVien() {
    controller.thongKe(KY, "  ");
    assertNull(dao.maNVDaLoc);
    controller.thongKe(KY, DUY.maNV());
    assertEquals(DUY.maNV(), dao.maNVDaLoc);
  }

  @Test
  @DisplayName("Kỳ rỗng: báo cáo không có dòng, tổng bằng 0 (6.a)")
  void kyRong() {
    dao.dongs = List.of();
    BaoCao baoCao = controller.thongKe(KY, null);
    assertEquals(0, baoCao.dong().size());
    assertEquals(0, baoCao.tongSoHoaDon());
    assertEquals(BigDecimal.ZERO, baoCao.tongSauDieuChinh());
  }

  @Test
  @DisplayName("Kỳ đảo ngược bị DateRange từ chối trước khi tới controller (5.a)")
  void kyKhongHopLe() {
    assertThrows(IllegalArgumentException.class, () -> DateRange.parse("31/03/2026", "01/01/2026"));
    assertThrows(IllegalArgumentException.class, () -> DateRange.parse("31/02/2026", "01/03/2026"));
  }

  @Test
  @DisplayName("Nhân viên bán hàng hoặc chưa đăng nhập không dùng được báo cáo")
  void chanSaiQuyen() {
    dangNhap(LoaiQuyen.NHAN_VIEN_BAN_HANG);
    assertThrows(SecurityException.class, () -> controller.thongKe(KY, null));
    assertThrows(SecurityException.class, () -> controller.doiChieu(KY, DUY.maNV()));
    assertThrows(SecurityException.class, controller::danhSachNhanVien);
    assertThrows(SecurityException.class, () -> controller.xuatExcel(new File("x.xlsx"), new BaoCao(KY, List.of())));
    CurrentSession.clear();
    assertThrows(SecurityException.class, () -> controller.thongKe(KY, null));
    assertEquals(0, dao.soLanDoc);
  }

  @Test
  @DisplayName("Xuất Excel đúng cột, đúng dòng đang hiển thị và có dòng tổng cộng")
  void xuatExcel(@TempDir Path thuMuc) throws Exception {
    BaoCao baoCao = controller.thongKe(KY, null);
    File tep = thuMuc.resolve("doanh-so.xlsx").toFile();

    assertNull(controller.xuatExcel(tep, baoCao));

    try (XSSFWorkbook workbook = new XSSFWorkbook(new FileInputStream(tep))) {
      Sheet sheet = workbook.getSheetAt(0);
      Row tieuDe = sheet.getRow(1);
      for (int i = 0; i < ThongKeNhanVienController.TIEU_DE_COT.size(); i++) {
        assertEquals(ThongKeNhanVienController.TIEU_DE_COT.get(i), tieuDe.getCell(i).getStringCellValue());
      }
      assertEquals(DUY.maNV(), sheet.getRow(2).getCell(0).getStringCellValue());
      assertEquals(17100000d, sheet.getRow(2).getCell(5).getNumericCellValue());
      Row tong = sheet.getRow(4);
      assertEquals("Tổng cộng", tong.getCell(0).getStringCellValue());
      assertEquals(37100000d, tong.getCell(5).getNumericCellValue());
      assertNull(sheet.getRow(5));
    }
  }

  @Test
  @DisplayName("Không ghi được tệp: trả thông báo lỗi, không ném lỗi ra giao diện")
  void xuatExcelLoiGhiTep(@TempDir Path thuMuc) {
    assertNotNull(controller.xuatExcel(thuMuc.toFile(), controller.thongKe(KY, null)));
  }

  private static void dangNhap(LoaiQuyen loaiQuyen) {
    CurrentSession.set(new NhanVien("NV", "Nhân viên", "0900000000", null,
        LocalDate.of(2026, 1, 1), "nv", "hash", loaiQuyen, true));
  }

  private static final class FakeThongKeDao implements ThongKeNhanVienDao {

    private List<DongThongKeNhanVien> dongs = List.of(DUY, MAI);
    private String maNVDaLoc;
    private int soLanDoc;

    @Override
    public List<DongThongKeNhanVien> thongKe(LocalDate tuNgay, LocalDate denNgay, String maNV) {
      soLanDoc++;
      maNVDaLoc = maNV;
      return dongs;
    }

    @Override
    public List<HoaDonDoiChieu> hoaDonTrongKy(LocalDate tuNgay, LocalDate denNgay, String maNV) {
      soLanDoc++;
      return List.of();
    }

    @Override
    public List<DieuChinhDoiChieu> dieuChinhTrongKy(LocalDate tuNgay, LocalDate denNgay, String maNV) {
      soLanDoc++;
      return List.of();
    }
  }

  private static final class FakeNhanVienDao implements NhanVienDao {

    @Override
    public List<NhanVien> findAll() {
      return List.of();
    }

    @Override
    public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void updateMatKhau(String maNV, String matKhauHash) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<NhanVien> findByMa(String maNV) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insert(Connection ketNoi, NhanVien nhanVien) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void update(NhanVien nhanVien) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void updateTrangThai(String maNV, boolean trangThaiHoatDong) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean existsTenDangNhap(String tenDangNhap) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean existsMaNV(String maNV) {
      throw new UnsupportedOperationException();
    }
  }
}
