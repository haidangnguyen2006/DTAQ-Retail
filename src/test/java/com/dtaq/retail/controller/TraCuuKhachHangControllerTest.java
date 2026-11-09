package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.KhachHangDao;
import com.dtaq.retail.dao.KhachHangGiaoDichDao;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietHoaDonView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.ChiTietPhieuView;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.HoaDonTomTat;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.LoaiPhieu;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.PhieuTomTat;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.session.CurrentSession;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tra cứu khách hàng và giao dịch (UC005)")
class TraCuuKhachHangControllerTest {

  private static final KhachHang AN = new KhachHang("KH-2026-000001", "Nguyễn Văn An", "0902100001", null);
  private static final KhachHang BINH = new KhachHang("KH-2026-000002", "Trần Thị Bình", "0902100002", null);
  private static final HoaDonTomTat HD1 = hoaDon("HD-2026-000001", LocalDate.of(2026, 1, 5));
  private static final HoaDonTomTat HD2 = hoaDon("HD-2026-000009", LocalDate.of(2026, 3, 2));
  private static final PhieuTomTat PT1 = new PhieuTomTat(LoaiPhieu.TRA, "PT-2026-000001", HD1.maHD(),
      LocalDate.of(2026, 1, 20), LocalDate.of(2026, 1, 22), TrangThaiDuyet.DA_DUYET,
      TrangThaiXuLyPhieu.HOAN_TAT, "Đồng ý");

  private final FakeGiaoDichDao giaoDichDao = new FakeGiaoDichDao();
  private final TraCuuKhachHangController controller =
      new TraCuuKhachHangController(new FakeKhachHangDao(), giaoDichDao);

  @BeforeEach
  void dangNhapBanHang() {
    dangNhap(LoaiQuyen.NHAN_VIEN_BAN_HANG);
  }

  @AfterEach
  void dangXuat() {
    CurrentSession.clear();
  }

  @Test
  @DisplayName("Tìm khách theo SĐT, bỏ qua khoảng trắng")
  void timKhachTheoSdt() {
    assertEquals(Optional.of(AN), controller.timKhach(" 0902 100 001 "));
  }

  @Test
  @DisplayName("SĐT không tồn tại hoặc để trống: không có kết quả (5.a)")
  void timKhachKhongThay() {
    assertTrue(controller.timKhach("0999999999").isEmpty());
    assertTrue(controller.timKhach("   ").isEmpty());
    assertTrue(controller.timKhach(null).isEmpty());
  }

  @Test
  @DisplayName("Khách có 2 hóa đơn và 1 phiếu trả: trả đủ lịch sử, chi tiết hóa đơn và nội dung phiếu")
  void khachCoHaiHoaDonVaMotPhieuTra() {
    assertEquals(List.of(HD2, HD1), controller.lichSuHoaDon(AN.maKH()));
    assertEquals(List.of(PT1), controller.phieuDoiTra(AN.maKH()));
    assertEquals(1, controller.chiTietHoaDon(HD1.maHD()).size());
    assertEquals(TinhTrangHangTra.HANG_LOI,
        controller.chiTietPhieu(LoaiPhieu.TRA, PT1.maPhieu()).get(0).tinhTrangHangTra());
  }

  @Test
  @DisplayName("Khách chưa có giao dịch: danh sách hóa đơn và phiếu trống (9.b)")
  void khachKhongGiaoDich() {
    assertTrue(controller.lichSuHoaDon(BINH.maKH()).isEmpty());
    assertTrue(controller.phieuDoiTra(BINH.maKH()).isEmpty());
  }

  @Test
  @DisplayName("Nhân viên quản lý hoặc chưa đăng nhập không tra cứu được")
  void chanSaiQuyen() {
    dangNhap(LoaiQuyen.NHAN_VIEN_QUAN_LY);
    assertThrows(SecurityException.class, () -> controller.timKhach(AN.soDienThoai()));
    assertThrows(SecurityException.class, () -> controller.lichSuHoaDon(AN.maKH()));
    assertThrows(SecurityException.class, () -> controller.chiTietHoaDon(HD1.maHD()));
    assertThrows(SecurityException.class, () -> controller.phieuDoiTra(AN.maKH()));
    assertThrows(SecurityException.class, () -> controller.chiTietPhieu(LoaiPhieu.TRA, PT1.maPhieu()));

    CurrentSession.clear();
    assertThrows(SecurityException.class, () -> controller.timKhach(AN.soDienThoai()));
    assertEquals(0, giaoDichDao.soLanDoc);
  }

  private static HoaDonTomTat hoaDon(String maHD, LocalDate ngayLap) {
    return new HoaDonTomTat(maHD, ngayLap, "Trần Văn Duy", 1, new BigDecimal("1000000"),
        new BigDecimal("100000"), new BigDecimal("1100000"), true);
  }

  private static void dangNhap(LoaiQuyen loaiQuyen) {
    CurrentSession.set(new NhanVien("NV", "Nhân viên", "0900000000", null,
        LocalDate.of(2026, 1, 1), "nv", "hash", loaiQuyen, true));
  }

  private static final class FakeKhachHangDao implements KhachHangDao {

    @Override
    public Optional<KhachHang> findBySdt(String soDienThoai) {
      return List.of(AN, BINH).stream().filter(kh -> kh.soDienThoai().equals(soDienThoai)).findFirst();
    }

    @Override
    public List<KhachHang> findAll() {
      return List.of(AN, BINH);
    }

    @Override
    public void insert(KhachHang khachHang) {
      throw new UnsupportedOperationException("tra cứu không ghi dữ liệu");
    }

    @Override
    public void insert(Connection ketNoi, KhachHang khachHang) {
      throw new UnsupportedOperationException("tra cứu không ghi dữ liệu");
    }

    @Override
    public void update(KhachHang khachHang) {
      throw new UnsupportedOperationException("tra cứu không ghi dữ liệu");
    }
  }

  private static final class FakeGiaoDichDao implements KhachHangGiaoDichDao {

    private int soLanDoc;

    @Override
    public List<HoaDonTomTat> hoaDonCuaKhach(String maKH) {
      soLanDoc++;
      return Map.of(AN.maKH(), List.of(HD2, HD1)).getOrDefault(maKH, List.of());
    }

    @Override
    public List<ChiTietHoaDonView> chiTietHoaDon(String maHD) {
      soLanDoc++;
      return List.of(new ChiTietHoaDonView("SAM-TV-0001", "DSP-2026-000001", "Tivi", new BigDecimal("1000000"),
          BigDecimal.TEN, BigDecimal.ZERO, new BigDecimal("1000000"), new BigDecimal("100000"),
          new BigDecimal("1100000")));
    }

    @Override
    public List<PhieuTomTat> phieuCuaKhach(String maKH) {
      soLanDoc++;
      return Map.of(AN.maKH(), List.of(PT1)).getOrDefault(maKH, List.of());
    }

    @Override
    public List<ChiTietPhieuView> chiTietPhieu(LoaiPhieu loai, String maPhieu) {
      soLanDoc++;
      return List.of(new ChiTietPhieuView("SAM-TV-0001", "Tivi", "Màn hình lỗi", TinhTrangHangTra.HANG_LOI, null));
    }
  }
}
