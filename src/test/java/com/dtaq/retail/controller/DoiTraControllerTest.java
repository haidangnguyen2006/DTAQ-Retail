package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ReturnConfig;
import com.dtaq.retail.controller.DoiTraController.KetQuaGhi;
import com.dtaq.retail.controller.DoiTraController.KetQuaTinh;
import com.dtaq.retail.controller.DoiTraController.KetQuaTraCuu;
import com.dtaq.retail.controller.DoiTraController.YeuCauDoiTra;
import com.dtaq.retail.dao.ChiTietHoaDonDao;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.dao.PhieuDoiDao;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.PhieuTraDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.ChiTietPhieuDoi;
import com.dtaq.retail.entity.ChiTietPhieuTra;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.PhieuDoi;
import com.dtaq.retail.entity.PhieuTra;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.SanPhamLoi;
import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.entity.TrangThaiXuLyLoi;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.session.CurrentSession;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử nghiệp vụ đổi/trả sản phẩm UC006 với DAO giả thủ công (không dùng Mockito). */
class DoiTraControllerTest {

  private static final ZoneId MUI_GIO = ZoneId.of("Asia/Ho_Chi_Minh");
  private static final Clock DONG_HO =
      Clock.fixed(Instant.parse("2026-11-04T03:00:00Z"), MUI_GIO);
  private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 4);

  private static final String MA_HD = "HD-2026-000001";
  private static final String SERIAL_TRA = "SAM-TV-0041";
  private static final String SERIAL_THAY_THE = "SAM-TV-0050";
  private static final String SERIAL_KHONG_THUOC = "SAM-TV-9999";

  private static final BigDecimal KHONG = BigDecimal.ZERO;

  private static final NhanVien BAN_HANG =
      nhanVien("NVBANHANG-2026-001", LoaiQuyen.NHAN_VIEN_BAN_HANG);
  private static final NhanVien QUAN_LY =
      nhanVien("NVQUANLY-2026-001", LoaiQuyen.NHAN_VIEN_QUAN_LY);
  private static final KhachHang KHACH_HANG =
      new KhachHang("KH-2026-000001", "Nguyễn Văn An", "0902100001", "Quận 1");

  private FakePhieuTraDao phieuTraDao;
  private FakePhieuDoiDao phieuDoiDao;
  private FakeSanPhamLoiDao sanPhamLoiDao;
  private FakeHoaDonDao hoaDonDao;
  private FakeChiTietHoaDonDao chiTietHoaDonDao;
  private FakeSanPhamDao sanPhamDao;
  private FakeDongSanPhamDao dongSanPhamDao;

  private DongSanPham dongTivi;
  private DongSanPham dongCaoCap;
  private DongSanPham dongPhoThong;
  private SanPham mayTra;
  private SanPham mayThayThe;
  private HoaDon hoaDon;
  private DoiTraController controller;
  private FakeDataSource dataSource;

  @BeforeEach
  void setUp() {
    CurrentSession.set(BAN_HANG);

    dataSource = new FakeDataSource();

    phieuTraDao = new FakePhieuTraDao();
    phieuDoiDao = new FakePhieuDoiDao();
    sanPhamLoiDao = new FakeSanPhamLoiDao();
    hoaDonDao = new FakeHoaDonDao();
    chiTietHoaDonDao = new FakeChiTietHoaDonDao();
    sanPhamDao = new FakeSanPhamDao();
    dongSanPhamDao = new FakeDongSanPhamDao();

    dongTivi = new DongSanPham(
        "DSP-2026-000001", "Tivi Samsung Crystal UHD 55 inch", null, "Chiếc", 5,
        BigDecimal.TEN, new BigDecimal("10000000"), true, HOM_NAY.minusDays(30), HOM_NAY);
    dongCaoCap = new DongSanPham(
        "DSP-2026-000002", "Tivi Samsung Neo QLED 65 inch", null, "Chiếc", 3,
        BigDecimal.TEN, new BigDecimal("12000000"), true, HOM_NAY.minusDays(30), HOM_NAY);
    dongPhoThong = new DongSanPham(
        "DSP-2026-000003", "Tivi Samsung Crystal UHD 43 inch", null, "Chiếc", 3,
        BigDecimal.TEN, new BigDecimal("8000000"), true, HOM_NAY.minusDays(30), HOM_NAY);
    for (DongSanPham dong : List.of(dongTivi, dongCaoCap, dongPhoThong)) {
      dongSanPhamDao.them(dong);
    }

    mayTra = may(SERIAL_TRA, dongTivi, TrangThaiSanPham.DA_BAN);
    mayThayThe = may(SERIAL_THAY_THE, dongCaoCap, TrangThaiSanPham.SAN_SANG_BAN);
    sanPhamDao.them(mayTra);
    sanPhamDao.them(mayThayThe);

    hoaDon = new HoaDon(MA_HD, HOM_NAY.minusDays(10), BAN_HANG, KHACH_HANG, true);
    datHoaDon(hoaDon, new BigDecimal("10000000"), new BigDecimal("1000000"));
    controller = taoController(60);
  }

  @AfterEach
  void tearDown() {
    CurrentSession.clear();
    System.clearProperty(ReturnConfig.KEY_SO_NGAY_DOI_TRA);
    ReturnConfig.setPropertiesFileForTest(null);
  }

  @Test
  @DisplayName("Cấu hình số ngày đổi/trả đọc được giá trị hợp lệ")
  void soNgayDoiTraTrongKhoangHopLe() {
    System.setProperty(ReturnConfig.KEY_SO_NGAY_DOI_TRA, "30");

    assertEquals(30, ReturnConfig.soNgayDoiTra());
  }

  @Test
  @DisplayName("Cấu hình số ngày đổi/trả trống nghĩa là không chặn thời hạn")
  void soNgayDoiTraTrongRongKhongChan() {
    System.setProperty(ReturnConfig.KEY_SO_NGAY_DOI_TRA, "");
    assertEquals(-1, ReturnConfig.soNgayDoiTra());

    datHoaDon(
        new HoaDon(MA_HD, HOM_NAY.minusDays(400), BAN_HANG, KHACH_HANG, true),
        new BigDecimal("2000000"), new BigDecimal("200000"));

    KetQuaTinh ketQua = taoController(-1).tinhPhieu(yeuCauTra());

    assertTrue(ketQua.hopLe(), "Không cấu hình thời hạn thì hóa đơn cũ vẫn được đổi/trả");
  }

  @Test
  @DisplayName("Tra cứu hóa đơn chưa thanh toán bị từ chối (RB23)")
  void traCuuHoaDonChuaThanhToanBiTuChoi() {
    datHoaDon(
        new HoaDon(MA_HD, HOM_NAY.minusDays(2), BAN_HANG, KHACH_HANG, false),
        new BigDecimal("10000000"), new BigDecimal("1000000"));

    KetQuaTraCuu ketQua = controller.traCuuHoaDon(MA_HD);

    assertFalse(ketQua.thanhCong());
    assertEquals("Hóa đơn chưa thanh toán, không đủ căn cứ xử lý đổi/trả", ketQua.thongBao());
    assertTrue(ketQua.mayDaBan().isEmpty());
  }

  @Test
  @DisplayName("Tra cứu hóa đơn trả về máy đã bán, giá trị và cờ đã đổi/trả")
  void traCuuHoaDonThanhCongTraVeMayDaBan() {
    phieuDoiDao.daDoiTra.add("SAM-TV-9999");

    KetQuaTraCuu ketQua = controller.traCuuHoaDon(" " + MA_HD + " ");

    assertTrue(ketQua.thanhCong());
    assertEquals(MA_HD, ketQua.maHD());
    assertEquals("Nguyễn Văn An", ketQua.tenKhachHang());
    assertEquals("0902100001", ketQua.soDienThoai());
    assertEquals(1, ketQua.mayDaBan().size());
    DoiTraController.MayDaBan may = ketQua.mayDaBan().get(0);
    assertEquals(SERIAL_TRA, may.soSerial());
    assertEquals("Tivi Samsung Crystal UHD 55 inch", may.tenDongSP());
    assertEquals(0, new BigDecimal("11000000").compareTo(may.giaTriThanhToan()));
    assertFalse(may.daDoiTra());
    assertEquals(TrangThaiSanPham.DA_BAN, may.trangThai());
  }

  @Test
  @DisplayName("Từ chối máy không thuộc hóa đơn gốc")
  void tuChoiMayKhongThuocHoaDon() {
    KetQuaTinh ketQua = controller.tinhPhieu(
        new YeuCauDoiTra(MA_HD, SERIAL_KHONG_THUOC, TinhTrangHangTra.NGUYEN_VEN,
            "Khách đổi ý", null));

    assertFalse(ketQua.hopLe());
    assertTrue(ketQua.thongBao().contains("không thuộc hóa đơn"));
  }

  @Test
  @DisplayName("Từ chối máy đã được đổi/trả trước đó (kể cả phiếu đổi)")
  void tuChoiMayDaDoiTra() {
    phieuDoiDao.daDoiTra.add(SERIAL_TRA);

    KetQuaTinh ketQua = controller.tinhPhieu(yeuCauTra());

    assertFalse(ketQua.hopLe());
    assertTrue(ketQua.thongBao().contains("đã được đổi/trả"));
  }

  @Test
  @DisplayName("Từ chối khi hóa đơn đã hết thời hạn đổi/trả (RB34)")
  void tuChoiKhiHetThoiHanDoiTra() {
    datHoaDon(
        new HoaDon(MA_HD, HOM_NAY.minusDays(31), BAN_HANG, KHACH_HANG, true),
        new BigDecimal("10000000"), new BigDecimal("1000000"));

    KetQuaTinh ketQua = taoController(30).tinhPhieu(yeuCauTra());

    assertFalse(ketQua.hopLe());
    assertEquals("Đã hết thời hạn đổi/trả (30 ngày)", ketQua.thongBao());
  }

  @Test
  @DisplayName("Giá trị máy trả bằng tiền sau khuyến mãi cộng tiền thuế, không làm tròn lại")
  void tienHoanBangTienSauKhuyenMaiCongThue() {
    datHoaDon(hoaDon, new BigDecimal("2000000"), new BigDecimal("200000"));

    KetQuaTinh ketQua = controller.tinhPhieu(yeuCauTra());

    assertTrue(ketQua.hopLe());
    assertEquals(0, new BigDecimal("2200000").compareTo(ketQua.giaTriMayTra()));
    assertEquals(0, new BigDecimal("2200000").compareTo(ketQua.soTienHoan()));
    assertEquals(0, new BigDecimal("-2200000").compareTo(ketQua.chenhLech()));
    assertEquals(0, KHONG.compareTo(ketQua.giaTriMayMoi()));
    assertFalse(ketQua.canDuyet());
  }

  @Test
  @DisplayName("Hoàn tiền từ 3.000.000 đồng trở lên thì chuyển chờ Quản lý duyệt")
  void hoanTienTuBaTrieuTroLenThiChoDuyet() {
    datHoaDon(hoaDon, new BigDecimal("3000000"), new BigDecimal("500000"));

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauTra());

    assertTrue(ketQua.thanhCong());
    assertTrue(ketQua.choDuyet());
    assertEquals("PT-2026-000099", ketQua.maPhieu());
    assertEquals(0, new BigDecimal("3500000").compareTo(ketQua.soTienHoan()));
    assertEquals(TrangThaiDuyet.CHO_DUYET, phieuTraDao.headerDaGhi.trangThaiDuyet());
    assertEquals(TrangThaiXuLyPhieu.CHO_XU_LY, phieuTraDao.headerDaGhi.trangThaiXuLyPhieu());
    assertNull(phieuTraDao.headerDaGhi.ngayHoanTat(),
        "Phiếu chờ duyệt chưa được ghi ngày hoàn tất");
    assertEquals(0, phieuTraDao.soLanDanhDau,
        "Phiếu chờ duyệt không được cập nhật trạng thái máy (UC006 10.a.4)");
    assertTrue(sanPhamLoiDao.daGhi.isEmpty());
  }

  @Test
  @DisplayName("Hoàn tiền dưới ngưỡng thì hoàn tất ngay và cập nhật trạng thái máy")
  void duoiNguongHoanTatNgay() {
    datHoaDon(hoaDon, new BigDecimal("900000"), new BigDecimal("100000"));

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauTra());

    assertTrue(ketQua.thanhCong());
    assertFalse(ketQua.choDuyet());
    assertEquals(TrangThaiDuyet.KHONG_CAN_DUYET, phieuTraDao.headerDaGhi.trangThaiDuyet());
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, phieuTraDao.headerDaGhi.trangThaiXuLyPhieu());
    assertEquals(HOM_NAY, phieuTraDao.headerDaGhi.ngayHoanTat());
    assertEquals(1, phieuTraDao.soLanDanhDau);
    assertEquals(0, new BigDecimal("1000000").compareTo(ketQua.soTienHoan()));
  }

  @Test
  @DisplayName("Đổi sang máy đắt hơn bị từ chối, không hoàn tiền (D17)")
  void doiMayGiaTriCaoHonBiTuChoi() {
    KetQuaTinh tinh = controller.tinhPhieu(yeuCauDoi(SERIAL_THAY_THE));

    assertFalse(tinh.hopLe());
    assertEquals(
        "Máy thay thế phải có giá trị ngang bằng hoặc thấp hơn máy trả; "
            + "đổi hàng không hoàn tiền",
        tinh.thongBao());

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauDoi(SERIAL_THAY_THE));

    assertFalse(ketQua.thanhCong());
    assertEquals(tinh.thongBao(), ketQua.thongBao());
    assertEquals(0, phieuDoiDao.soLanTaoPhieuDoi, "Không được tạo phiếu đổi");
    assertEquals(0, phieuDoiDao.soLanDanhDau);
    assertEquals(0, phieuDoiDao.soLanDanhDauThayThe);
  }

  @Test
  @DisplayName("Đổi sang máy rẻ hơn: chênh lệch âm, không hoàn tiền, hoàn tất ngay")
  void doiMayGiaTriThapHonKhongHoanTien() {
    SanPham mayRe = may("SAM-TV-0052", dongPhoThong, TrangThaiSanPham.SAN_SANG_BAN);
    sanPhamDao.them(mayRe);

    KetQuaTinh tinh = controller.tinhPhieu(yeuCauDoi(mayRe.soSerial()));
    assertTrue(tinh.hopLe());
    assertEquals(0, new BigDecimal("8800000").compareTo(tinh.giaTriMayMoi()));
    assertEquals(0, new BigDecimal("-2200000").compareTo(tinh.chenhLech()));
    assertEquals(0, KHONG.compareTo(tinh.soTienHoan()));
    assertFalse(tinh.canDuyet());

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauDoi(mayRe.soSerial()));

    assertTrue(ketQua.thanhCong());
    assertFalse(ketQua.choDuyet());
    assertEquals(0, KHONG.compareTo(ketQua.soTienHoan()));
    assertEquals(0, new BigDecimal("-2200000").compareTo(ketQua.chenhLech()));
    assertEquals(0, KHONG.compareTo(phieuDoiDao.headerDaGhi.tienKhachDuaThem()));
    assertEquals(0, new BigDecimal("-2200000")
        .compareTo(phieuDoiDao.headerDaGhi.tienChenhLech()));
    assertEquals(TrangThaiDuyet.KHONG_CAN_DUYET, phieuDoiDao.headerDaGhi.trangThaiDuyet());
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, phieuDoiDao.headerDaGhi.trangThaiXuLyPhieu());
    assertNotNull(phieuDoiDao.headerDaGhi.ngayHoanTat());
    assertEquals(1, phieuDoiDao.soLanDanhDau);
    assertEquals(1, phieuDoiDao.soLanDanhDauThayThe);
    assertTrue(ketQua.thongBao().contains("không hoàn lại"));
  }

  @Test
  @DisplayName("Đổi ngang giá: chênh lệch 0 và hoàn tất ngay")
  void doiMayNgangGiaKhongHoanTien() {
    DongSanPham dongNgangGia = new DongSanPham(
        "DSP-2026-000004", "Tivi ngang giá", null, "Chiếc", 2,
        BigDecimal.TEN, new BigDecimal("10000000"), true, HOM_NAY.minusDays(10), HOM_NAY);
    dongSanPhamDao.them(dongNgangGia);
    SanPham mayNgangGia = may("SAM-TV-0054", dongNgangGia, TrangThaiSanPham.SAN_SANG_BAN);
    sanPhamDao.them(mayNgangGia);

    KetQuaTinh tinh = controller.tinhPhieu(yeuCauDoi(mayNgangGia.soSerial()));
    assertTrue(tinh.hopLe());
    assertEquals(0, KHONG.compareTo(tinh.chenhLech()));
    assertEquals(0, KHONG.compareTo(tinh.soTienHoan()));
    assertFalse(tinh.canDuyet());

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauDoi(mayNgangGia.soSerial()));

    assertTrue(ketQua.thanhCong());
    assertFalse(ketQua.choDuyet());
    assertEquals(0, KHONG.compareTo(ketQua.chenhLech()));
    assertEquals(0, KHONG.compareTo(phieuDoiDao.headerDaGhi.tienKhachDuaThem()));
    assertEquals(0, KHONG.compareTo(phieuDoiDao.headerDaGhi.tienChenhLech()));
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, phieuDoiDao.headerDaGhi.trangThaiXuLyPhieu());
    assertEquals(1, phieuDoiDao.soLanDanhDau);
    assertEquals(1, phieuDoiDao.soLanDanhDauThayThe);
  }

  @Test
  @DisplayName("Đổi hàng có chênh lệch lớn vẫn không vào trạng thái chờ duyệt")
  void doiMayKhongBaoGioVaoChoDuyet() {
    DongSanPham dongMotTrieu = new DongSanPham(
        "DSP-2026-000099", "Tivi giá rẻ", null, "Chiếc", 1, KHONG,
        new BigDecimal("1000000"), true, HOM_NAY, HOM_NAY);
    dongSanPhamDao.them(dongMotTrieu);
    SanPham mayMotTrieu = may("SAM-TV-0099", dongMotTrieu, TrangThaiSanPham.SAN_SANG_BAN);
    sanPhamDao.them(mayMotTrieu);
    datHoaDon(hoaDon, new BigDecimal("5000000"), KHONG);

    KetQuaTinh tinh = controller.tinhPhieu(yeuCauDoi(mayMotTrieu.soSerial()));
    assertTrue(tinh.hopLe());
    assertEquals(0, new BigDecimal("-4000000").compareTo(tinh.chenhLech()));
    assertEquals(0, KHONG.compareTo(tinh.soTienHoan()));
    assertFalse(tinh.canDuyet(), "Ngưỡng 3.000.000 chỉ áp cho phiếu trả");

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauDoi(mayMotTrieu.soSerial()));
    assertTrue(ketQua.thanhCong());
    assertFalse(ketQua.choDuyet());
    assertEquals(TrangThaiDuyet.KHONG_CAN_DUYET, phieuDoiDao.headerDaGhi.trangThaiDuyet());
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, phieuDoiDao.headerDaGhi.trangThaiXuLyPhieu());
  }

  @Test
  @DisplayName("Máy thay thế không sẵn sàng bán thì bị từ chối (UC006 8.b)")
  void mayThayTheKhongSanSangBanBiTuChoi() {
    mayThayThe = may(SERIAL_THAY_THE, dongCaoCap, TrangThaiSanPham.DA_BAN);
    sanPhamDao.them(mayThayThe);

    KetQuaTinh ketQua = controller.tinhPhieu(yeuCauDoi(SERIAL_THAY_THE));

    assertFalse(ketQua.hopLe());
    assertTrue(ketQua.thongBao().contains("không sẵn sàng bán"));
  }

  @Test
  @DisplayName("Máy thay thế chỉ có mã dòng thì nạp lại dòng sản phẩm để tính giá")
  void mayThayTheChiCoMaDongThiNapLaiDongSanPham() {
    DongSanPham chiCoMa = new DongSanPham(
        dongPhoThong.maDongSP(), null, null, null, 0, null, null, true, null, null);
    SanPham mayThieuGia =
        new SanPham("SAM-TV-0053", null, chiCoMa, KHONG, HOM_NAY,
            TrangThaiSanPham.SAN_SANG_BAN, 0, 0);
    sanPhamDao.them(mayThieuGia);

    KetQuaTinh ketQua = controller.tinhPhieu(yeuCauDoi(mayThieuGia.soSerial()));

    assertTrue(ketQua.hopLe());
    assertEquals(0, new BigDecimal("8800000").compareTo(ketQua.giaTriMayMoi()),
        "Phải dùng đơn giá và thuế của DongSanPhamDao khi entity máy thiếu giá");
  }

  @Test
  @DisplayName("Phiếu chưa được Quản lý phê duyệt thì không được hoàn tất (UC006 13.a)")
  void hoanTatPhieuChuaDuyetBiTuChoi() {
    PhieuTra phieu = phieuTra("PT-2026-000010", TrangThaiDuyet.CHO_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY, new BigDecimal("3500000"));
    phieuTraDao.phieuById.put(phieu.maPhieuTra(), phieu);
    phieuTraDao.chiTietById.put(
        phieu.maPhieuTra(),
        List.of(new ChiTietPhieuTra(phieu, dongHoaDon(), TinhTrangHangTra.NGUYEN_VEN,
            "Khách đổi ý")));

    KetQuaGhi ketQua = controller.hoanTatPhieu(phieu.maPhieuTra());

    assertFalse(ketQua.thanhCong());
    assertEquals("Phiếu chưa được Quản lý phê duyệt, không thể hoàn tất", ketQua.thongBao());
    assertEquals(0, phieuTraDao.soLanDanhDau);
    assertEquals(0, phieuTraDao.soLanCapNhatHoanTat);
  }

  @Test
  @DisplayName("Hoàn tất phiếu đã duyệt: cập nhật máy, ghi hàng lỗi và đóng phiếu")
  void hoanTatPhieuDaDuyetThanhCong() {
    PhieuTra phieu = phieuTra("PT-2026-000011", TrangThaiDuyet.DA_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY, new BigDecimal("3500000"));
    phieuTraDao.phieuById.put(phieu.maPhieuTra(), phieu);
    phieuTraDao.chiTietById.put(
        phieu.maPhieuTra(),
        List.of(new ChiTietPhieuTra(phieu, dongHoaDon(), TinhTrangHangTra.HANG_LOI,
            "Máy không lên nguồn")));

    KetQuaGhi ketQua = controller.hoanTatPhieu(phieu.maPhieuTra());

    assertTrue(ketQua.thanhCong());
    assertEquals(phieu.maPhieuTra(), ketQua.maPhieu());
    assertEquals(1, phieuTraDao.soLanDanhDau);
    assertEquals(1, phieuTraDao.soLanCapNhatHoanTat);
    assertEquals(1, sanPhamLoiDao.daGhi.size());
    assertEquals(phieu.maPhieuTra(), sanPhamLoiDao.daGhi.get(0).phieuTra().maPhieuTra());
    assertEquals("Máy không lên nguồn", sanPhamLoiDao.daGhi.get(0).moTaLoi());
  }

  @Test
  @DisplayName("Hoàn tất phiếu đổi đã duyệt cập nhật máy nhận lại và máy thay thế")
  void hoanTatPhieuDoiDaDuyetThanhCong() {
    PhieuDoi phieu = new PhieuDoi(
        "PD-2026-000010",
        BAN_HANG,
        hoaDon,
        null,
        HOM_NAY.minusDays(1),
        TrangThaiDuyet.DA_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY,
        KHONG,
        new BigDecimal("-2000000"),
        null,
        null,
        null);
    phieuDoiDao.phieuById.put(phieu.maPhieuDoi(), phieu);
    phieuDoiDao.chiTietById.put(
        phieu.maPhieuDoi(),
        List.of(new ChiTietPhieuDoi(
            phieu, dongHoaDon(), "Khách đổi sang mẫu khác", TinhTrangHangTra.NGUYEN_VEN,
            mayThayThe)));

    KetQuaGhi ketQua = controller.hoanTatPhieu(phieu.maPhieuDoi());

    assertTrue(ketQua.thanhCong());
    assertEquals(phieu.maPhieuDoi(), ketQua.maPhieu());
    assertEquals(0, new BigDecimal("-2000000").compareTo(ketQua.chenhLech()));
    assertEquals(0, BigDecimal.ZERO.compareTo(ketQua.soTienHoan()),
        "D17: phiếu đổi hoàn tất không hoàn tiền");
    assertEquals(1, phieuDoiDao.soLanDanhDau);
    assertEquals(1, phieuDoiDao.soLanDanhDauThayThe);
    assertEquals(1, phieuDoiDao.soLanCapNhatHoanTat);
    assertTrue(sanPhamLoiDao.daGhi.isEmpty());
  }

  @Test
  @DisplayName("Hoàn tất phiếu đổi tồn đọng không hoàn tiền (D17)")
  void hoanTatPhieuDoiTonDongKhongHoanTien() {
    PhieuDoi phieu = new PhieuDoi(
        "PD-2026-000011",
        BAN_HANG,
        hoaDon,
        null,
        HOM_NAY.minusDays(2),
        TrangThaiDuyet.DA_DUYET,
        TrangThaiXuLyPhieu.CHO_XU_LY,
        KHONG,
        new BigDecimal("-1500000"),
        null,
        null,
        null);
    phieuDoiDao.phieuById.put(phieu.maPhieuDoi(), phieu);
    phieuDoiDao.chiTietById.put(
        phieu.maPhieuDoi(),
        List.of(new ChiTietPhieuDoi(
            phieu, dongHoaDon(), "Khách đổi sang mẫu khác", TinhTrangHangTra.NGUYEN_VEN,
            mayThayThe)));

    KetQuaGhi ketQua = controller.hoanTatPhieu(phieu.maPhieuDoi());

    assertTrue(ketQua.thanhCong());
    assertFalse(ketQua.choDuyet());
    assertEquals(0, BigDecimal.ZERO.compareTo(ketQua.soTienHoan()),
        "Phiếu đổi lịch sử không bao giờ hoàn tiền dù chênh lệch âm");
    assertEquals(0, new BigDecimal("-1500000").compareTo(ketQua.chenhLech()),
        "Chênh lệch vẫn được giữ để hiển thị");
    assertEquals(1, phieuDoiDao.soLanDanhDau);
    assertEquals(1, phieuDoiDao.soLanDanhDauThayThe);
    assertEquals(1, phieuDoiDao.soLanCapNhatHoanTat);
  }

  @Test
  @DisplayName("Ghi phiếu trả hàng lỗi dưới ngưỡng có tạo hồ sơ SanPhamLoi")
  void ghiPhieuTraHangLoiGhiNhanSanPhamLoi() {
    datHoaDon(hoaDon, new BigDecimal("900000"), new BigDecimal("100000"));

    KetQuaGhi ketQua = controller.ghiPhieu(new YeuCauDoiTra(
        MA_HD, SERIAL_TRA, TinhTrangHangTra.HANG_LOI, "Máy lỗi nguồn", null));

    assertTrue(ketQua.thanhCong());
    assertEquals(1, sanPhamLoiDao.daGhi.size());
    SanPhamLoi spl = sanPhamLoiDao.daGhi.get(0);
    assertEquals(SERIAL_TRA, spl.sanPham().soSerial());
    assertEquals("PT-2026-000099", spl.phieuTra().maPhieuTra());
    assertNull(spl.phieuDoi());
    assertEquals(TrangThaiXuLyLoi.CHO_XU_LY, spl.tinhTrangXuLy());
    assertEquals(HOM_NAY, spl.ngayGhiNhan());
  }

  @Test
  @DisplayName("Đổi máy dưới ngưỡng cập nhật cả máy nhận lại và máy thay thế")
  void doiMayDuoiNguongCapNhatHaiMay() {
    DongSanPham dongNgangGia = new DongSanPham(
        "DSP-2026-000004", "Tivi ngang giá", null, "Chiếc", 2,
        BigDecimal.TEN, new BigDecimal("10000000"), true, HOM_NAY.minusDays(10), HOM_NAY);
    dongSanPhamDao.them(dongNgangGia);
    SanPham mayNgangGia = may("SAM-TV-0054", dongNgangGia, TrangThaiSanPham.SAN_SANG_BAN);
    sanPhamDao.them(mayNgangGia);

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauDoi(mayNgangGia.soSerial()));

    assertTrue(ketQua.thanhCong());
    assertFalse(ketQua.choDuyet());
    assertEquals(0, KHONG.compareTo(ketQua.chenhLech()));
    assertEquals(0, KHONG.compareTo(ketQua.soTienHoan()));
    assertEquals(1, phieuDoiDao.soLanDanhDau);
    assertEquals(1, phieuDoiDao.soLanDanhDauThayThe);
    assertEquals(0, KHONG.compareTo(phieuDoiDao.headerDaGhi.tienKhachDuaThem()));
    assertEquals(0, KHONG.compareTo(phieuDoiDao.headerDaGhi.tienChenhLech()));
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, phieuDoiDao.headerDaGhi.trangThaiXuLyPhieu());
  }

  @Test
  @DisplayName("Danh sách chờ hoàn tất gộp hai loại phiếu và sắp xếp theo ngày tạo rồi mã phiếu")
  void danhSachChoHoanTatGopVaSapXep() {
    phieuTraDao.choHoanTat.add(phieuDuyet(PhieuDuyet.LoaiPhieu.PHIEU_TRA,
        "PT-2026-000012", LocalDate.of(2026, 6, 2)));
    phieuDoiDao.choHoanTat.add(phieuDuyet(PhieuDuyet.LoaiPhieu.PHIEU_DOI,
        "PD-2026-000009", LocalDate.of(2026, 6, 1)));

    List<PhieuDuyet> danhSach = controller.danhSachChoHoanTat();

    assertEquals(List.of("PD-2026-000009", "PT-2026-000012"),
        danhSach.stream().map(PhieuDuyet::maPhieu).toList());
    assertThrows(UnsupportedOperationException.class,
        () -> danhSach.add(phieuDuyet(PhieuDuyet.LoaiPhieu.PHIEU_TRA, "PT-2026-000013", HOM_NAY)));
  }

  @Test
  @DisplayName("Chỉ Nhân viên bán hàng được tra cứu, lập và hoàn tất phiếu")
  void chiNhanVienBanHangDuocLapPhieu() {
    CurrentSession.set(QUAN_LY);
    SecurityException loi = assertThrows(SecurityException.class,
        () -> controller.traCuuHoaDon(MA_HD));
    assertEquals("Chỉ Nhân viên bán hàng được xử lý đổi trả", loi.getMessage());
    assertThrows(SecurityException.class, () -> controller.tinhPhieu(yeuCauTra()));
    assertThrows(SecurityException.class, () -> controller.ghiPhieu(yeuCauTra()));
    assertThrows(SecurityException.class, () -> controller.hoanTatPhieu("PT-2026-000001"));
    assertThrows(SecurityException.class, controller::danhSachChoHoanTat);

    CurrentSession.set(BAN_HANG);
    assertTrue(controller.traCuuHoaDon(MA_HD).thanhCong());
  }

  @Test
  @DisplayName("Hoàn tiền đúng 3.000.000 đồng vẫn phải chờ Quản lý phê duyệt")
  void hoanTienDungBaTrieuThiChoDuyet() {
    datHoaDon(hoaDon, new BigDecimal("2500000"), new BigDecimal("500000"));

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauTra());

    assertTrue(ketQua.thanhCong());
    assertTrue(ketQua.choDuyet());
    assertEquals(0, new BigDecimal("3000000").compareTo(ketQua.soTienHoan()));
    assertEquals(TrangThaiDuyet.CHO_DUYET, phieuTraDao.headerDaGhi.trangThaiDuyet());
    assertEquals(TrangThaiXuLyPhieu.CHO_XU_LY, phieuTraDao.headerDaGhi.trangThaiXuLyPhieu());
    assertEquals(0, phieuTraDao.soLanDanhDau,
        "Phiếu chờ duyệt không được cập nhật trạng thái máy");
  }

  @Test
  @DisplayName("Hoàn tiền 2.999.999 đồng hoàn tất ngay và cập nhật máy đúng một lần")
  void hoanTienDuoiBaTrieuMotDongThiHoanTatNgay() {
    datHoaDon(hoaDon, new BigDecimal("2499999"), new BigDecimal("500000"));

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauTra());

    assertTrue(ketQua.thanhCong());
    assertFalse(ketQua.choDuyet());
    assertEquals(0, new BigDecimal("2999999").compareTo(ketQua.soTienHoan()));
    assertEquals(TrangThaiXuLyPhieu.HOAN_TAT, phieuTraDao.headerDaGhi.trangThaiXuLyPhieu());
    assertEquals(HOM_NAY, phieuTraDao.headerDaGhi.ngayHoanTat());
    assertEquals(1, phieuTraDao.soLanDanhDau);
  }

  @Test
  @DisplayName("DAO báo máy đã đổi/trả thì ghi phiếu trả về kết quả thất bại")
  void ghiPhieuTraVeKetQuaThatBaiKhiDaoBaoMayDaDoiTra() {
    phieuTraDao.loiKhiTaoPhieu = new IllegalStateException(
        "Máy " + SERIAL_TRA + " đã được đổi/trả trước đó");

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauTra());

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("đã được đổi/trả trước đó"));
    assertNull(ketQua.maPhieu());
    assertEquals(1, dataSource.soLanRollback, "Giao dịch phải được quay lui");
  }

  @Test
  @DisplayName("Ghi phiếu thất bại giữa transaction thì rollback và khôi phục auto-commit")
  void ghiPhieuRollbackVaKhoiPhucAutoCommitKhiDaoNem() {
    phieuTraDao.loiKhiTaoPhieu = new IllegalStateException("Máy đã có phiếu hiệu lực");

    KetQuaGhi ketQua = controller.ghiPhieu(yeuCauTra());

    assertFalse(ketQua.thanhCong());
    assertTrue(dataSource.daTatAutoCommit, "Phải tắt auto-commit trước khi ghi");
    assertTrue(dataSource.daKhoiPhucAutoCommit, "Phải khôi phục auto-commit sau khi lỗi");
    assertEquals(1, dataSource.soLanRollback, "Phải rollback đúng một lần");
    assertEquals(0, dataSource.soLanCommit, "Không được commit khi DAO ném lỗi");
  }

  @Test
  @DisplayName("Phiếu đã hoàn tất không bị báo nhầm là chưa được Quản lý duyệt")
  void hoanTatPhieuBaoDaHoanTatKhiPhieuDaLàHoanTat() {
    PhieuTra phieu = phieuTra(
        "PT-2026-000020",
        TrangThaiDuyet.KHONG_CAN_DUYET,
        TrangThaiXuLyPhieu.HOAN_TAT,
        new BigDecimal("1000000"));
    phieuTraDao.phieuById.put(phieu.maPhieuTra(), phieu);
    phieuTraDao.chiTietById.put(
        phieu.maPhieuTra(),
        List.of(new ChiTietPhieuTra(
            phieu, dongHoaDon(), TinhTrangHangTra.NGUYEN_VEN, "Khách đổi ý")));

    KetQuaGhi ketQua = controller.hoanTatPhieu(phieu.maPhieuTra());

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("đã được hoàn tất"));
    assertFalse(ketQua.thongBao().contains("chưa được Quản lý phê duyệt"));
    assertEquals(0, phieuTraDao.soLanDanhDau);
    assertEquals(0, phieuTraDao.soLanCapNhatHoanTat);
  }

  private DoiTraController taoController(int soNgayDoiTraToiDa) {
    return new DoiTraController(
        phieuTraDao,
        phieuDoiDao,
        sanPhamLoiDao,
        hoaDonDao,
        chiTietHoaDonDao,
        sanPhamDao,
        dongSanPhamDao,
        dataSource,
        soNgayDoiTraToiDa,
        DONG_HO);
  }

  private void datHoaDon(HoaDon hoaDon, BigDecimal tienSauKhuyenMai, BigDecimal tienThue) {
    hoaDonDao.them(hoaDon);
    chiTietHoaDonDao.ds.clear();
    chiTietHoaDonDao.ds.add(new ChiTietHoaDon(
        hoaDon,
        mayTra,
        1,
        new BigDecimal("10000000"),
        BigDecimal.TEN,
        null,
        null,
        KHONG,
        new BigDecimal("10000000"),
        tienSauKhuyenMai,
        tienThue));
  }

  private ChiTietHoaDon dongHoaDon() {
    return chiTietHoaDonDao.ds.get(0);
  }

  private YeuCauDoiTra yeuCauTra() {
    return new YeuCauDoiTra(MA_HD, SERIAL_TRA, TinhTrangHangTra.NGUYEN_VEN,
        "Khách đổi ý, máy nguyên vẹn", null);
  }

  private YeuCauDoiTra yeuCauDoi(String serialMayThayThe) {
    return new YeuCauDoiTra(MA_HD, SERIAL_TRA, TinhTrangHangTra.NGUYEN_VEN,
        "Khách đổi sang mẫu khác", serialMayThayThe);
  }

  private PhieuTra phieuTra(
      String maPhieu,
      TrangThaiDuyet trangThaiDuyet,
      TrangThaiXuLyPhieu trangThaiXuLy,
      BigDecimal tienKhachDua) {
    return new PhieuTra(
        maPhieu, BAN_HANG, hoaDon, HOM_NAY.minusDays(1), HOM_NAY, trangThaiDuyet, trangThaiXuLy,
        null, null, null, tienKhachDua);
  }

  private static PhieuDuyet phieuDuyet(
      PhieuDuyet.LoaiPhieu loai, String maPhieu, LocalDate ngayTao) {
    return new PhieuDuyet(loai, maPhieu, MA_HD, "Nguyễn Văn An", "0902100001",
        BigDecimal.ZERO, "Trần Văn Duy", "Khách đổi ý", ngayTao);
  }

  private static NhanVien nhanVien(String maNV, LoaiQuyen loaiQuyen) {
    return new NhanVien(maNV, "Nhân viên " + maNV, "0900000001", maNV + "@dtaq.example",
        LocalDate.of(2026, 1, 1), maNV, "matkhau", loaiQuyen, true);
  }

  private static SanPham may(
      String soSerial, DongSanPham dongSanPham, TrangThaiSanPham trangThai) {
    return new SanPham(soSerial, null, dongSanPham, KHONG, HOM_NAY.minusDays(20),
        trangThai, 0, 0);
  }

  /** DAO phiếu trả giả ghi lại tham số của lần gọi gần nhất. */
  private static final class FakePhieuTraDao implements PhieuTraDao {

    private final Set<String> daDoiTra = new HashSet<>();
    private final Map<String, PhieuTra> phieuById = new HashMap<>();
    private final Map<String, List<ChiTietPhieuTra>> chiTietById = new HashMap<>();
    private final List<PhieuDuyet> choHoanTat = new ArrayList<>();
    private PhieuTra headerDaGhi;
    private List<ChiTietPhieuTra> chiTietDaGhi;
    private RuntimeException loiKhiTaoPhieu;
    private int soLanDanhDau;
    private int soLanCapNhatHoanTat;
    private boolean ketQuaCapNhatHoanTat = true;

    @Override
    public Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials) {
      Set<String> ketQua = new HashSet<>();
      for (String serial : soSerials) {
        if (daDoiTra.contains(serial)) {
          ketQua.add(serial);
        }
      }
      return ketQua;
    }

    @Override
    public Optional<PhieuTra> findById(String maPhieuTra) {
      return Optional.ofNullable(phieuById.get(maPhieuTra));
    }

    @Override
    public List<ChiTietPhieuTra> findChiTiet(String maPhieuTra) {
      return chiTietById.getOrDefault(maPhieuTra, List.of());
    }

    @Override
    public List<PhieuDuyet> findChoHoanTat() {
      return choHoanTat;
    }

    @Override
    public String taoPhieuTra(
        Connection ketNoi, PhieuTra phieuTra, List<ChiTietPhieuTra> chiTiet) {
      if (loiKhiTaoPhieu != null) {
        throw loiKhiTaoPhieu;
      }
      headerDaGhi = phieuTra;
      chiTietDaGhi = chiTiet;
      return "PT-2026-000099";
    }

    @Override
    public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuTra> chiTiet) {
      soLanDanhDau++;
      chiTietDaGhi = chiTiet;
      return chiTiet.size();
    }

    @Override
    public boolean capNhatHoanTat(
        Connection ketNoi, String maPhieuTra, LocalDate ngayHoanTat) {
      soLanCapNhatHoanTat++;
      return ketQuaCapNhatHoanTat;
    }

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }
  }

  /** DAO phiếu đổi giả ghi lại tham số của lần gọi gần nhất. */
  private static final class FakePhieuDoiDao implements PhieuDoiDao {

    private final Set<String> daDoiTra = new HashSet<>();
    private final Map<String, PhieuDoi> phieuById = new HashMap<>();
    private final Map<String, List<ChiTietPhieuDoi>> chiTietById = new HashMap<>();
    private final List<PhieuDuyet> choHoanTat = new ArrayList<>();
    private PhieuDoi headerDaGhi;
    private List<ChiTietPhieuDoi> chiTietDaGhi;
    private int soLanTaoPhieuDoi;
    private int soLanDanhDau;
    private int soLanDanhDauThayThe;
    private int soLanCapNhatHoanTat;
    private boolean ketQuaCapNhatHoanTat = true;

    @Override
    public Set<String> findSerialsDaDoiTra(String maHD, Collection<String> soSerials) {
      Set<String> ketQua = new HashSet<>();
      for (String serial : soSerials) {
        if (daDoiTra.contains(serial)) {
          ketQua.add(serial);
        }
      }
      return ketQua;
    }

    @Override
    public Optional<PhieuDoi> findById(String maPhieuDoi) {
      return Optional.ofNullable(phieuById.get(maPhieuDoi));
    }

    @Override
    public List<ChiTietPhieuDoi> findChiTiet(String maPhieuDoi) {
      return chiTietById.getOrDefault(maPhieuDoi, List.of());
    }

    @Override
    public List<PhieuDuyet> findChoHoanTat() {
      return choHoanTat;
    }

    @Override
    public String taoPhieuDoi(
        Connection ketNoi, PhieuDoi phieuDoi, List<ChiTietPhieuDoi> chiTiet) {
      soLanTaoPhieuDoi++;
      headerDaGhi = phieuDoi;
      chiTietDaGhi = chiTiet;
      return "PD-2026-000099";
    }

    @Override
    public int danhDauMayNhanLai(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
      soLanDanhDau++;
      chiTietDaGhi = chiTiet;
      return chiTiet.size();
    }

    @Override
    public int danhDauMayThayThe(Connection ketNoi, List<ChiTietPhieuDoi> chiTiet) {
      soLanDanhDauThayThe++;
      return chiTiet.size();
    }

    @Override
    public boolean capNhatHoanTat(
        Connection ketNoi, String maPhieuDoi, LocalDate ngayHoanTat) {
      soLanCapNhatHoanTat++;
      return ketQuaCapNhatHoanTat;
    }

    @Override
    public List<PhieuDuyet> findChoDuyet() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public boolean capNhatQuyetDinh(
        String maPhieu,
        TrangThaiDuyet quyetDinh,
        String maNhanVienDuyet,
        LocalDate ngayDuyet,
        String yKien) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }
  }

  /** DAO sản phẩm lỗi giả chỉ hỗ trợ ghi trong transaction. */
  private static final class FakeSanPhamLoiDao implements SanPhamLoiDao {

    private final List<SanPhamLoi> daGhi = new ArrayList<>();

    @Override
    public void insert(Connection ketNoi, SanPhamLoi sanPhamLoi) {
      daGhi.add(sanPhamLoi);
    }

    @Override
    public void insert(SanPhamLoi sanPhamLoi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public Optional<SanPhamLoi> findById(String maLoi) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public Optional<SanPhamLoi> findDangXuLy(String soSerial) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<SanPhamLoi> findAll() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<SanPhamLoi> findByTinhTrang(TrangThaiXuLyLoi tinhTrangXuLy) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public void updateTinhTrang(
        Connection ketNoi,
        String maLoi,
        TrangThaiXuLyLoi tinhTrangMoi,
        String ghiChuXuLy,
        LocalDate ngayCapNhat) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public void updateTinhTrang(
        String maLoi,
        TrangThaiXuLyLoi tinhTrangMoi,
        String ghiChuXuLy,
        LocalDate ngayCapNhat) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }
  }

  /** DAO hóa đơn giả trả về hóa đơn đã nạp. */
  private static final class FakeHoaDonDao implements HoaDonDao {

    private final Map<String, HoaDon> theoMa = new HashMap<>();

    private void them(HoaDon hoaDon) {
      theoMa.put(hoaDon.maHD(), hoaDon);
    }

    @Override
    public Optional<HoaDon> findById(String maHD) {
      return Optional.ofNullable(theoMa.get(maHD));
    }

    @Override
    public void insert(Connection ketNoi, HoaDon hoaDon) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<HoaDon> findDaThanhToanTrongKy(LocalDate tuNgay, LocalDate denNgay) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }
  }

  /** DAO chi tiết hóa đơn giả trả về danh sách đã nạp. */
  private static final class FakeChiTietHoaDonDao implements ChiTietHoaDonDao {

    private final List<ChiTietHoaDon> ds = new ArrayList<>();

    @Override
    public List<ChiTietHoaDon> findByHoaDon(String maHD) {
      return ds.stream()
          .filter(dong -> maHD.equals(dong.hoaDon().maHD()))
          .toList();
    }

    @Override
    public void insert(Connection ketNoi, ChiTietHoaDon chiTiet) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }
  }

  /** DAO sản phẩm giả trả về máy đã nạp. */
  private static final class FakeSanPhamDao implements SanPhamDao {

    private final Map<String, SanPham> theoSerial = new HashMap<>();

    private void them(SanPham sanPham) {
      theoSerial.put(sanPham.soSerial(), sanPham);
    }

    @Override
    public Optional<SanPham> findById(String soSerial) {
      return Optional.ofNullable(theoSerial.get(soSerial));
    }

    @Override
    public void insert(Connection ketNoi, SanPham sanPham) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<SanPham> findByDongSanPham(String maDongSP) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<SanPham> findByTrangThai(TrangThaiSanPham trangThai) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<SanPham> findAll() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public void updateTrangThai(String soSerial, TrangThaiSanPham trangThai) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }
  }

  /** DAO dòng sản phẩm giả trả về dòng đã nạp. */
  private static final class FakeDongSanPhamDao implements DongSanPhamDao {

    private final Map<String, DongSanPham> theoMa = new HashMap<>();

    private void them(DongSanPham dongSanPham) {
      theoMa.put(dongSanPham.maDongSP(), dongSanPham);
    }

    @Override
    public Optional<DongSanPham> findById(String maDongSP) {
      return Optional.ofNullable(theoMa.get(maDongSP));
    }

    @Override
    public void insert(Connection ketNoi, DongSanPham dongSanPham) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<DongSanPham> findAll() {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public List<DongSanPham> findByDanhMuc(String maDanhMuc) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public void updateSoLuongTon(String maDongSP, int soLuongTon) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }

    @Override
    public void updateTrangThaiKinhDoanh(String maDongSP, boolean trangThaiKinhDoanh) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử UC006");
    }
  }

  /** Nguồn dữ liệu giả cấp một Connection không thực thi SQL, chỉ đủ cho transaction. */
  private static final class FakeDataSource implements DataSource {

    private int soLanRollback;
    private int soLanCommit;
    private boolean daTatAutoCommit;
    private boolean daKhoiPhucAutoCommit;

    private final Connection ketNoi = (Connection) Proxy.newProxyInstance(
        FakeDataSource.class.getClassLoader(),
        new Class<?>[] {Connection.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getAutoCommit" -> true;
          case "prepareStatement" -> taoPreparedStatement();
          case "setAutoCommit" -> {
            boolean bat = (boolean) args[0];
            if (!bat) {
              daTatAutoCommit = true;
            } else if (daTatAutoCommit) {
              daKhoiPhucAutoCommit = true;
            }
            yield null;
          }
          case "commit" -> {
            soLanCommit++;
            yield null;
          }
          case "rollback" -> {
            soLanRollback++;
            yield null;
          }
          case "close" -> null;
          case "isClosed" -> false;
          case "toString" -> "fake-connection";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> null;
        });

    /**
     * Câu lệnh giả đủ để {@code MaGenerator} đọc được MAX(mã) = 0 và sinh mã đầu tiên của năm.
     *
     * <p>DAO giả không dùng {@link Connection} nên chỉ bước sinh mã của {@code SanPhamLoi} cần
     * đối tượng này; không có SQL thật nào được thực thi.
     */
    private static PreparedStatement taoPreparedStatement() {
      return (PreparedStatement) Proxy.newProxyInstance(
          FakeDataSource.class.getClassLoader(),
          new Class<?>[] {PreparedStatement.class},
          (proxy, method, args) -> switch (method.getName()) {
            case "executeQuery" -> taoResultSet();
            case "executeUpdate" -> 1;
            case "toString" -> "fake-prepared-statement";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
          });
    }

    private static ResultSet taoResultSet() {
      boolean[] daDoc = {false};
      return (ResultSet) Proxy.newProxyInstance(
          FakeDataSource.class.getClassLoader(),
          new Class<?>[] {ResultSet.class},
          (proxy, method, args) -> switch (method.getName()) {
            case "next" -> {
              boolean con = !daDoc[0];
              daDoc[0] = true;
              yield con;
            }
            case "getLong" -> 0L;
            case "getInt" -> 0;
            case "toString" -> "fake-result-set";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> null;
          });
    }

    @Override
    public Connection getConnection() {
      return ketNoi;
    }

    @Override
    public Connection getConnection(String username, String password) {
      return ketNoi;
    }

    @Override
    public java.io.PrintWriter getLogWriter() {
      return null;
    }

    @Override
    public void setLogWriter(java.io.PrintWriter out) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public void setLoginTimeout(int seconds) {
      // Không dùng trong kiểm thử.
    }

    @Override
    public int getLoginTimeout() {
      return 0;
    }

    @Override
    public java.util.logging.Logger getParentLogger() {
      return java.util.logging.Logger.getGlobal();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
      throw new SQLException("Không hỗ trợ unwrap");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
      return false;
    }
  }
}
