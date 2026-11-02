package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.dao.impl.SanPhamImpl;
import com.dtaq.retail.dao.impl.SanPhamLoiDaoImpl;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử đơn vị và tích hợp cho bộ điều khiển sản phẩm lỗi.
 */
class SanPhamLoiControllerTest {

  private static final String SERIAL_TEST = "SAM-TV-0002"; // máy có sẵn từ seed V4
  private static final String PHIEU_DOI_SEED = "PD-2026-000001"; // phiếu đổi của máy khác PAN-AC-0013

  private SanPhamLoiDao sanPhamLoiDao;
  private SanPhamDao sanPhamDao;
  private SanPhamLoiController controller;

  private final List<String> maLoiCanXoa = new ArrayList<>();
  private TrangThaiSanPham trangThaiGocCuaMay;

  @BeforeEach
  void setUp() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "Cơ sở dữ liệu không khả dụng");
    sanPhamLoiDao = new SanPhamLoiDaoImpl(ConnectDB.getDataSource());
    sanPhamDao = new SanPhamImpl(ConnectDB.getDataSource());
    controller = new SanPhamLoiController(sanPhamLoiDao, sanPhamDao, ConnectDB.getDataSource());

    // Lưu lại trạng thái gốc của máy test để khôi phục
    Optional<SanPham> sp = sanPhamDao.findById(SERIAL_TEST);
    sp.ifPresent(sanPham -> trangThaiGocCuaMay = sanPham.trangThai());

    cleanUpTestData();
  }

  @AfterEach
  void tearDown() {
    if (ConnectDB.isAvailable()) {
      cleanUpTestData();
      if (trangThaiGocCuaMay != null) {
        sanPhamDao.updateTrangThai(SERIAL_TEST, trangThaiGocCuaMay);
      }
    }
  }

  private void cleanUpTestData() {
    try (Connection conn = ConnectDB.getDataSource().getConnection();
        var ps = conn.prepareStatement("DELETE FROM \"SanPhamLoi\" WHERE \"soSerial\" = ? AND \"maLoi\" <> 'SPL-2026-000001'")) {
      ps.setString(1, SERIAL_TEST);
      ps.executeUpdate();
    } catch (Exception ignored) {
    }
    maLoiCanXoa.clear();
  }

  @Test
  @DisplayName("Lấy danh sách sản phẩm lỗi toàn bộ và lọc theo trạng thái")
  void layDanhSach_toanBoVaTheoTrangThai() {
    List<SanPhamLoi> toanBo = controller.layDanhSach(null);
    assertNotNull(toanBo);
    assertFalse(toanBo.isEmpty());

    List<SanPhamLoi> choXuLy = controller.layDanhSach(TrangThaiXuLyLoi.CHO_XU_LY);
    assertNotNull(choXuLy);
    for (SanPhamLoi spl : choXuLy) {
      assertEquals(TrangThaiXuLyLoi.CHO_XU_LY, spl.tinhTrangXuLy());
    }
  }

  @Test
  @DisplayName("Tìm kiếm hồ sơ lỗi theo mã")
  void timTheoMa_thanhCongVaKhongTonTai() {
    Optional<SanPhamLoi> tonTai = controller.timTheoMa("SPL-2026-000001");
    assertTrue(tonTai.isPresent());

    Optional<SanPhamLoi> khongTonTai = controller.timTheoMa("SPL-KHONG-TON-TAI");
    assertTrue(khongTonTai.isEmpty());

    assertTrue(controller.timTheoMa(null).isEmpty());
    assertTrue(controller.timTheoMa("   ").isEmpty());
  }

  @Test
  @DisplayName("Ghi nhận sản phẩm lỗi thành công và đồng bộ trạng thái máy vật lý")
  void ghiNhanLoi_thanhCong() {
    sanPhamDao.updateTrangThai(SERIAL_TEST, TrangThaiSanPham.SAN_SANG_BAN);

    SanPhamLoiController.KetQua<SanPhamLoi> ketQua = controller.ghiNhanLoi(
        SERIAL_TEST,
        "Màn hình bị nháy đen liên tục",
        LocalDate.now(),
        "Ghi chú kiểm tra tại quầy",
        null,
        null);

    assertTrue(ketQua.thanhCong());
    assertNotNull(ketQua.duLieu());
    String maLoi = ketQua.duLieu().maLoi();
    maLoiCanXoa.add(maLoi);

    assertTrue(maLoi.matches("^SPL-\\d{4}-\\d{6}$"));
    assertEquals(TrangThaiXuLyLoi.CHO_XU_LY, ketQua.duLieu().tinhTrangXuLy());

    // Kiểm tra máy vật lý đã được đồng bộ sang HANG_LOI
    Optional<SanPham> maySauLoi = sanPhamDao.findById(SERIAL_TEST);
    assertTrue(maySauLoi.isPresent());
    assertEquals(TrangThaiSanPham.HANG_LOI, maySauLoi.get().trangThai());
  }

  @Test
  @DisplayName("Từ chối ghi nhận lỗi khi thông tin bắt buộc bị để trống")
  void ghiNhanLoi_thongTinRong() {
    SanPhamLoiController.KetQua<SanPhamLoi> loiSerial = controller.ghiNhanLoi(
        "   ", "Mô tả lỗi", LocalDate.now(), null, null, null);
    assertFalse(loiSerial.thanhCong());
    assertTrue(loiSerial.thongBao().contains("Số serial"));

    SanPhamLoiController.KetQua<SanPhamLoi> loiMoTa = controller.ghiNhanLoi(
        SERIAL_TEST, "   ", LocalDate.now(), null, null, null);
    assertFalse(loiMoTa.thanhCong());
    assertTrue(loiMoTa.thongBao().contains("Mô tả"));
  }

  @Test
  @DisplayName("Từ chối ghi nhận lỗi khi ngày phát hiện ở tương lai")
  void ghiNhanLoi_ngayTuongLai() {
    SanPhamLoiController.KetQua<SanPhamLoi> ketQua = controller.ghiNhanLoi(
        SERIAL_TEST,
        "Mô tả lỗi",
        LocalDate.now().plusDays(2),
        null,
        null,
        null);

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("ngày hiện tại"));
  }

  @Test
  @DisplayName("Từ chối ghi nhận lỗi khi máy không tồn tại trong hệ thống")
  void ghiNhanLoi_mayKhongTonTai() {
    SanPhamLoiController.KetQua<SanPhamLoi> ketQua = controller.ghiNhanLoi(
        "SERIAL-KHONG-CO-THAT",
        "Mô tả lỗi",
        LocalDate.now(),
        null,
        null,
        null);

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("Không tìm thấy sản phẩm"));
  }

  @Test
  @DisplayName("Chặn tạo hồ sơ lỗi trùng khi máy đang có hồ sơ chờ xử lý")
  void ghiNhanLoi_chanTrungHoSoDangXuLy() {
    SanPhamLoiController.KetQua<SanPhamLoi> lan1 = controller.ghiNhanLoi(
        SERIAL_TEST,
        "Lần 1 phát hiện lỗi",
        LocalDate.now(),
        null,
        null,
        null);
    assertTrue(lan1.thanhCong());
    maLoiCanXoa.add(lan1.duLieu().maLoi());

    SanPhamLoiController.KetQua<SanPhamLoi> lan2 = controller.ghiNhanLoi(
        SERIAL_TEST,
        "Lần 2 thử tạo tiếp",
        LocalDate.now(),
        null,
        null,
        null);
    assertFalse(lan2.thanhCong());
    assertTrue(lan2.thongBao().contains("đang trong quá trình xử lý"));
  }

  @Test
  @DisplayName("Bắt lỗi khi gán chứng từ đổi trả không chứa số serial của máy")
  void ghiNhanLoi_saiChungTuLienKet() {
    SanPhamLoiController.KetQua<SanPhamLoi> ketQua = controller.ghiNhanLoi(
        SERIAL_TEST,
        "Lỗi gán nhầm chứng từ",
        LocalDate.now(),
        null,
        null,
        PHIEU_DOI_SEED);

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("Chứng từ liên kết không hợp lệ")
        || ketQua.thongBao().contains("serial"));
  }

  @Test
  @DisplayName("Chuyển trạng thái xử lý lỗi thành công và đồng bộ trạng thái máy vật lý")
  void chuyenTrangThai_thanhCongDongBoSanPham() {
    SanPhamLoiController.KetQua<SanPhamLoi> taoMoi = controller.ghiNhanLoi(
        SERIAL_TEST,
        "Máy lỗi cần gửi bảo hành",
        LocalDate.now(),
        null,
        null,
        null);
    assertTrue(taoMoi.thanhCong());
    String maLoi = taoMoi.duLieu().maLoi();
    maLoiCanXoa.add(maLoi);

    // Chuyển sang DA_GUI_BAO_HANH
    SanPhamLoiController.KetQua<Void> chuyenTrangThai = controller.chuyenTrangThai(
        maLoi,
        TrangThaiXuLyLoi.DA_GUI_BAO_HANH,
        "Đã bàn giao cho trung tâm bảo hành hãng");
    assertTrue(chuyenTrangThai.thanhCong());

    // Kiểm tra hồ sơ lỗi
    Optional<SanPhamLoi> sauChuyen = controller.timTheoMa(maLoi);
    assertTrue(sauChuyen.isPresent());
    assertEquals(TrangThaiXuLyLoi.DA_GUI_BAO_HANH, sauChuyen.get().tinhTrangXuLy());
    assertEquals("Đã bàn giao cho trung tâm bảo hành hãng", sauChuyen.get().ghiChuXuLy());

    // Kiểm tra máy vật lý đồng bộ sang DANG_BAO_HANH
    Optional<SanPham> maySauChuyen = sanPhamDao.findById(SERIAL_TEST);
    assertTrue(maySauChuyen.isPresent());
    assertEquals(TrangThaiSanPham.DANG_BAO_HANH, maySauChuyen.get().trangThai());
  }

  @Test
  @DisplayName("Từ chối chuyển trạng thái khi mã lỗi không tồn tại")
  void chuyenTrangThai_maLoiKhongTonTai() {
    SanPhamLoiController.KetQua<Void> ketQua = controller.chuyenTrangThai(
        "SPL-KHONG-TON-TAI",
        TrangThaiXuLyLoi.DA_TRA_NHA_CUNG_CAP,
        "Ghi chú");
    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("Không tìm thấy"));
  }
}
