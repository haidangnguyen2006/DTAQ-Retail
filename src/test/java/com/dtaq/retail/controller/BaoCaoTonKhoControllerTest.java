package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.BaoCaoTonKhoDao;
import com.dtaq.retail.dao.BaoCaoTonKhoDao.DongTonLau;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.exception.DataAccessException;
import com.dtaq.retail.session.CurrentSession;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("Nghiệp vụ báo cáo tồn kho lâu ngày (UC014)")
class BaoCaoTonKhoControllerTest {

  @TempDir Path tempDir;

  private FakeBaoCaoTonKhoDao dao;
  private BaoCaoTonKhoController controller;

  @BeforeEach
  void setUp() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    dao = new FakeBaoCaoTonKhoDao();
    controller = new BaoCaoTonKhoController(dao);
  }

  @AfterEach
  void dongPhien() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Nhân viên bán hàng không được xem báo cáo tồn kho")
  void khongPhaiQuanLyThiChanXemBaoCao() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));

    assertThrows(SecurityException.class, () -> controller.xemBaoCao(30, true));
    assertEquals(0, dao.soLanTruyVan);
  }

  @Test
  @DisplayName("Nhân viên bán hàng không được xuất Excel báo cáo tồn kho")
  void khongPhaiQuanLyThiChanXuatExcel() {
    CurrentSession.set(nhanVien(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    File file = tempDir.resolve("bi-chan.xlsx").toFile();

    assertThrows(SecurityException.class,
        () -> controller.xuatExcel(file, List.of(dong("S-1", "DSP-1", 90))));
    assertFalse(file.exists());
  }

  private static NhanVien nhanVien(LoaiQuyen loaiQuyen) {
    return new NhanVien(
        "NVQUANLY-TEST-001",
        "Nguyễn Quản Lý",
        "0900000001",
        "quanly.test@dtaq.local",
        LocalDate.of(2026, 1, 1),
        "quanly.test",
        "matkhau-test",
        loaiQuyen,
        true);
  }

  @Test
  @DisplayName("Từ chối ngưỡng bằng không hoặc âm mà không gọi DAO")
  void xemBaoCao_nguongKhongHopLe() {
    assertFalse(controller.xemBaoCao(0, false).thanhCong());
    assertFalse(controller.xemBaoCao(-1, true).thanhCong());
    assertEquals(0, dao.soLanTruyVan);
  }

  @Test
  @DisplayName("Tổng hợp số máy, số dòng sản phẩm và số ngày tồn lớn nhất")
  void xemBaoCao_tongHopKpi() {
    dao.rows = List.of(
        dong("S-1", "DSP-1", 90),
        dong("S-2", "DSP-1", 35),
        dong("S-3", "DSP-2", 60));

    var result = controller.xemBaoCao(30, true);

    assertTrue(result.thanhCong());
    assertEquals(3, result.duLieu().tongSoMay());
    assertEquals(2, result.duLieu().soDongSanPham());
    assertEquals(90, result.duLieu().soNgayTonLonNhat());
    assertEquals(dao.rows, result.duLieu().chiTiet());
    assertEquals(30, dao.nguongNgayCuoi);
    assertTrue(dao.chiTinhSanSangBanCuoi);
  }

  @Test
  @DisplayName("Danh sách rỗng tạo KPI bằng không")
  void xemBaoCao_danhSachRong() {
    var result = controller.xemBaoCao(60, false);

    assertTrue(result.thanhCong());
    assertTrue(result.duLieu().chiTiet().isEmpty());
    assertEquals(0, result.duLieu().tongSoMay());
    assertEquals(0, result.duLieu().soDongSanPham());
    assertEquals(0, result.duLieu().soNgayTonLonNhat());
  }

  @Test
  @DisplayName("Trả kết quả lỗi khi DAO không truy vấn được")
  void xemBaoCao_loiDao() {
    dao.loiTruyVan = true;

    var result = controller.xemBaoCao(60, false);

    assertFalse(result.thanhCong());
    assertTrue(result.thongBao().contains("Không thể tải báo cáo"));
    assertEquals(null, result.duLieu());
  }

  @Test
  @DisplayName("Xuất Excel thành công từ danh sách hiện có")
  void xuatExcel_thanhCong() throws Exception {
    List<DongTonLau> displayedRows = List.of(dong("S-1", "DSP-1", 90));
    File file = tempDir.resolve("ton-kho.xlsx").toFile();

    var result = controller.xuatExcel(file, displayedRows);

    assertTrue(result.thanhCong());
    assertTrue(Files.size(file.toPath()) > 0);
    assertEquals(1, displayedRows.size());
  }

  @Test
  @DisplayName("Lỗi ghi Excel trả thông báo nhưng giữ nguyên dữ liệu báo cáo")
  void xuatExcel_loiGhiFile() {
    List<DongTonLau> displayedRows = List.of(dong("S-1", "DSP-1", 90));
    List<DongTonLau> snapshot = List.copyOf(displayedRows);
    File file = tempDir.resolve("missing-directory").resolve("ton-kho.xlsx").toFile();

    var result = controller.xuatExcel(file, displayedRows);

    assertFalse(result.thanhCong());
    assertTrue(result.thongBao().contains("Không thể xuất tệp Excel"));
    assertEquals(snapshot, displayedRows);
  }

  private static DongTonLau dong(String serial, String maDongSP, long soNgayTon) {
    return new DongTonLau(serial, maDongSP, "Dòng " + maDongSP, "Danh mục",
        LocalDate.of(2026, 1, 1), TrangThaiSanPham.SAN_SANG_BAN, soNgayTon);
  }

  private static final class FakeBaoCaoTonKhoDao implements BaoCaoTonKhoDao {
    List<DongTonLau> rows = List.of();
    int soLanTruyVan;
    int nguongNgayCuoi;
    boolean chiTinhSanSangBanCuoi;
    boolean loiTruyVan;

    @Override
    public List<DongTonLau> findTonLau(int nguongNgay, boolean chiTinhSanSangBan) {
      soLanTruyVan++;
      nguongNgayCuoi = nguongNgay;
      chiTinhSanSangBanCuoi = chiTinhSanSangBan;
      if (loiTruyVan) {
        throw new DataAccessException("lỗi DB giả lập");
      }
      return new ArrayList<>(rows);
    }
  }
}
