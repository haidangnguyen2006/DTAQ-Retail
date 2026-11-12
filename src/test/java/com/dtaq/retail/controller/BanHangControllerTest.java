package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.dao.ChiTietHoaDonDao;
import com.dtaq.retail.dao.HoaDonDao;
import com.dtaq.retail.dao.HoaDonTamDao;
import com.dtaq.retail.dao.KhuyenMaiDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.entity.ChiTietHoaDon;
import com.dtaq.retail.entity.DanhMucSanPham;
import com.dtaq.retail.entity.DongSanPham;
import com.dtaq.retail.entity.HoaDon;
import com.dtaq.retail.entity.KhachHang;
import com.dtaq.retail.entity.KhuyenMai;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhaCungCap;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import com.dtaq.retail.exception.DataAccessException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử nghiệp vụ lập hóa đơn bán hàng UC003 (đợt W1) theo dòng sản phẩm và số lượng. */
class BanHangControllerTest {

  private static final DanhMucSanPham DANH_MUC = new DanhMucSanPham("DM-01", "Laptop", "Máy tính xách tay");
  private static final NhaCungCap NHA_CUNG_CAP = new NhaCungCap("NCC-01", "Nhà cung cấp A");

  private static final String DSP_1 = "DSP-2026-000001";
  private static final String DSP_2 = "DSP-2026-000002";
  private static final String DSP_3 = "DSP-2026-000003";

  @Test
  @DisplayName("timDongSanPham: tìm theo mã và tên, không phân biệt hoa thường, chỉ trả dòng còn kinh doanh")
  void timDongSanPhamTheoMaVaTen() {
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt Samsung", 20000000, "10", true);
    DongSanPham tuLanh = dong(DSP_2, "Tủ lạnh", 15000000, "5", true);
    DongSanPham dongNgung = dong(DSP_3, "Máy giặt cũ", 1000000, "10", false);
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000002", mayGiat, TrangThaiSanPham.DA_BAN));
    sanPhamDao.them(may("SP-2026-000003", tuLanh, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000004", dongNgung, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());

    List<BanHangController.DongBan> theoMa = controller.timDongSanPham("dsp-2026-000001");
    assertEquals(1, theoMa.size());
    assertEquals(DSP_1, theoMa.getFirst().maDongSP());
    assertEquals("Máy giặt Samsung", theoMa.getFirst().tenDongSP());
    assertEquals(1, theoMa.getFirst().tonKho(), "chỉ đếm máy đang sẵn sàng bán");
    assertTien(20000000, theoMa.getFirst().donGiaTruocThue());
    assertTien(10, theoMa.getFirst().thueSuat());

    List<BanHangController.DongBan> theoTen = controller.timDongSanPham("TỦ LẠNH");
    assertEquals(1, theoTen.size());
    assertEquals(DSP_2, theoTen.getFirst().maDongSP());

    List<BanHangController.DongBan> theoChuoiChung = controller.timDongSanPham("máy");
    assertEquals(List.of(DSP_1), theoChuoiChung.stream()
        .map(BanHangController.DongBan::maDongSP).toList(),
        "dòng đã ngừng kinh doanh không được trả về");

    assertTrue(controller.timDongSanPham("  ").isEmpty());
    assertTrue(controller.timDongSanPham(null).isEmpty());
  }

  @Test
  @DisplayName("timDongSanPham: tối đa 20 dòng sản phẩm mỗi lần tra cứu")
  void timDongSanPhamGioiHanHaiMuoiDong() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    for (int i = 1; i <= 25; i++) {
      DongSanPham dong = dong(String.format("DSP-2026-%06d", i), "Điều hòa " + i,
          10000000, "8", true);
      sanPhamDao.them(may(String.format("SP-2026-%06d", i), dong, TrangThaiSanPham.SAN_SANG_BAN));
    }
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());

    assertEquals(20, controller.timDongSanPham("điều hòa").size());
  }

  @Test
  @DisplayName("themDong: thêm mới rồi cộng dồn số lượng của cùng dòng sản phẩm")
  void themDongMoiVaCongDon() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000005, "10", true);
    for (int i = 1; i <= 5; i++) {
      sanPhamDao.them(may(String.format("SP-2026-%06d", i), mayGiat,
          TrangThaiSanPham.SAN_SANG_BAN));
    }
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());

    BanHangController.KetQuaDoiDong lanMot = controller.themDong(DSP_1, 2);

    assertTrue(lanMot.thanhCong());
    assertEquals(1, lanMot.hoaDon().dongHang().size());
    assertEquals(2, lanMot.hoaDon().dongHang().getFirst().soLuong());
    assertEquals(5, lanMot.hoaDon().dongHang().getFirst().tonKho());
    assertTien(2000010, lanMot.hoaDon().tongTruocThue());

    BanHangController.KetQuaDoiDong lanHai = controller.themDong(DSP_1, 1);

    assertTrue(lanHai.thanhCong());
    assertEquals(1, lanHai.hoaDon().dongHang().size());
    assertEquals(3, lanHai.hoaDon().dongHang().getFirst().soLuong());
    assertTien(3000015, lanHai.hoaDon().tongTruocThue());
    assertTien(300003, lanHai.hoaDon().tongThue());
    assertTien(3300018, lanHai.hoaDon().tongThanhToan());
  }

  @Test
  @DisplayName("themDong: chặn cộng dồn vượt quá số máy đang bán được")
  void themDongChanVuotTon() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000002", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());
    controller.themDong(DSP_1, 2);

    BanHangController.KetQuaDoiDong ketQua = controller.themDong(DSP_1, 1);

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("chỉ còn 2 máy"));
    assertEquals(2, ketQua.hoaDon().dongHang().getFirst().soLuong());
  }

  @Test
  @DisplayName("themDong: báo hết hàng và báo dòng đã ngừng kinh doanh")
  void themDongBaoHetHangVaNgungKinhDoanh() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham daBanHet = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    DongSanPham dongNgung = dong(DSP_2, "Tủ lạnh", 2000000, "8", false);
    sanPhamDao.them(may("SP-2026-000001", daBanHet, TrangThaiSanPham.DA_BAN));
    sanPhamDao.them(may("SP-2026-000002", dongNgung, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());

    BanHangController.KetQuaDoiDong hetHang = controller.themDong(DSP_1, 1);
    BanHangController.KetQuaDoiDong ngungKinhDoanh = controller.themDong(DSP_2, 1);
    BanHangController.KetQuaDoiDong khongTonTai = controller.themDong("DSP-2026-999999", 1);

    assertFalse(hetHang.thanhCong());
    assertTrue(hetHang.thongBao().contains("đã hết hàng"));
    assertFalse(ngungKinhDoanh.thanhCong());
    assertTrue(ngungKinhDoanh.thongBao().contains("không còn kinh doanh"));
    assertFalse(khongTonTai.thanhCong());
    assertTrue(khongTonTai.hoaDon().dongHang().isEmpty());
    assertThrows(IllegalArgumentException.class, () -> controller.themDong(DSP_1, 0));
  }

  @Test
  @DisplayName("datSoLuong: chặn số lượng 0 và số lượng vượt tồn kho")
  void datSoLuongChanBien() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    for (int i = 1; i <= 3; i++) {
      sanPhamDao.them(may(String.format("SP-2026-%06d", i), mayGiat,
          TrangThaiSanPham.SAN_SANG_BAN));
    }
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());
    controller.themDong(DSP_1, 2);

    BanHangController.KetQuaDoiDong khongHopLe = controller.datSoLuong(DSP_1, 0);
    BanHangController.KetQuaDoiDong vuotTon = controller.datSoLuong(DSP_1, 4);
    BanHangController.KetQuaDoiDong hopLe = controller.datSoLuong(DSP_1, 3);

    assertFalse(khongHopLe.thanhCong());
    assertTrue(khongHopLe.thongBao().contains("tối thiểu là 1"));
    assertFalse(vuotTon.thanhCong());
    assertTrue(vuotTon.thongBao().contains("chỉ còn 3 máy"));
    assertTrue(hopLe.thanhCong());
    assertEquals(3, hopLe.hoaDon().dongHang().getFirst().soLuong());
    assertThrows(IllegalArgumentException.class, () -> controller.datSoLuong(DSP_2, 1));
  }

  @Test
  @DisplayName("boDong: bỏ dòng thì tổng tiền được tính lại")
  void boDongTinhLaiTong() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000005, "10", true);
    DongSanPham tuLanh = dong(DSP_2, "Tủ lạnh", 999, "5", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000002", tuLanh, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());
    controller.themDong(DSP_1, 1);
    controller.themDong(DSP_2, 1);

    BanHangController.HoaDonTinh hoaDon = controller.boDong(DSP_2);

    assertEquals(1, hoaDon.dongHang().size());
    assertEquals(DSP_1, hoaDon.dongHang().getFirst().maDongSP());
    assertTien(1000005, hoaDon.tongTruocThue());
    assertTien(100001, hoaDon.tongThue());
    assertTien(1100006, hoaDon.tongThanhToan());
  }

  @Test
  @DisplayName("tinhDong: làm tròn theo đơn vị máy rồi mới nhân với số lượng")
  void tinhDongTheoDonViMay() {
    BanHangController.DongTinh dong =
        BanHangController.tinhDong(new BigDecimal("1000005"), new BigDecimal("10"), 3);

    assertTien(3000015, dong.tienTruocKhuyenMai());
    assertTien(0, dong.tienKhuyenMai());
    assertTien(3000015, dong.tienSauKhuyenMai());
    assertTien(300003, dong.tienThue());
  }

  @Test
  @DisplayName("phanBoMay: chọn máy nhập kho cũ nhất trước, cùng ngày thì theo số serial")
  void phanBoMayCuNhatTruoc() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000004", mayGiat, TrangThaiSanPham.SAN_SANG_BAN,
        LocalDate.of(2026, 1, 10)));
    sanPhamDao.them(may("SP-2026-000003", mayGiat, TrangThaiSanPham.SAN_SANG_BAN,
        LocalDate.of(2026, 1, 5)));
    sanPhamDao.them(may("SP-2026-000002", mayGiat, TrangThaiSanPham.SAN_SANG_BAN,
        LocalDate.of(2026, 1, 5)));
    sanPhamDao.them(may("SP-2026-000005", mayGiat, TrangThaiSanPham.SAN_SANG_BAN,
        LocalDate.of(2026, 1, 20)));
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.DA_BAN,
        LocalDate.of(2026, 1, 1)));
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());
    controller.themDong(DSP_1, 3);

    BanHangController.KetQuaPhanBo phanBo = controller.phanBoMay();

    assertEquals(
        List.of("SP-2026-000002", "SP-2026-000003", "SP-2026-000004"),
        phanBo.mayDuocChon().stream()
            .map(BanHangController.MayDuocChon::soSerial).toList(),
        "01-05 (serial nhỏ trước) rồi 01-10; máy đã bán không bao giờ được chọn");
    assertTrue(phanBo.dongThieu().isEmpty());
    assertEquals(DSP_1, phanBo.mayDuocChon().getFirst().maDongSP());
    assertEquals(LocalDate.of(2026, 1, 5), phanBo.mayDuocChon().getFirst().ngayNhapKhoBanDau());

    sanPhamDao.xoaMay("SP-2026-000004");
    sanPhamDao.xoaMay("SP-2026-000005");

    BanHangController.KetQuaPhanBo thieuMay = controller.phanBoMay();

    assertEquals(2, thieuMay.mayDuocChon().size());
    assertEquals(List.of(DSP_1), thieuMay.dongThieu());
  }

  @Test
  @DisplayName("phanBoMay: không chọn máy của dòng đã ngừng kinh doanh và báo dòng thiếu máy")
  void phanBoMayBoQuaDongNgungKinhDoanh() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham tuLanh = dong(DSP_2, "Tủ lạnh", 2000000, "8", true);
    sanPhamDao.them(may("SP-2026-000001", tuLanh, TrangThaiSanPham.SAN_SANG_BAN,
        LocalDate.of(2026, 1, 1)));
    BanHangController controller = new BanHangController(sanPhamDao, new FakeHoaDonTamDao());
    controller.themDong(DSP_2, 1);

    sanPhamDao.doiDong(DSP_2, dong(DSP_2, "Tủ lạnh", 2000000, "8", false));
    BanHangController.KetQuaPhanBo phanBo = controller.phanBoMay();

    assertTrue(phanBo.mayDuocChon().isEmpty());
    assertEquals(List.of(DSP_2), phanBo.dongThieu());
  }

  @Test
  @DisplayName("Lưu hóa đơn tạm theo dòng rồi khôi phục, điều chỉnh theo tồn kho hiện tại")
  void luuBanTamTheoDongRoiKhoiPhuc() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    FakeHoaDonTamDao hoaDonTamDao = new FakeHoaDonTamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000005, "10", true);
    DongSanPham tuLanh = dong(DSP_2, "Tủ lạnh", 2000000, "5", true);
    for (int i = 1; i <= 3; i++) {
      sanPhamDao.them(may(String.format("SP-2026-%06d", i), mayGiat,
          TrangThaiSanPham.SAN_SANG_BAN));
    }
    sanPhamDao.them(may("SP-2026-000010", tuLanh, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = new BanHangController(sanPhamDao, hoaDonTamDao);
    controller.themDong(DSP_1, 3);
    controller.themDong(DSP_2, 1);
    KhachHang khachHang = new KhachHang("KH-2026-000001", "Trần Thị B", "0901234567", "Cần Thơ");

    HoaDonTamDao.HoaDonTam banTam =
        controller.luuBanTam("NV-2026-000002", "Nguyễn Văn A", khachHang);

    assertNotNull(banTam.maTam());
    assertEquals(banTam.maTam(), controller.maTamHienTai());
    assertEquals("KH-2026-000001", banTam.maKH());
    assertEquals("Trần Thị B", banTam.tenKhachHang());
    assertEquals("0901234567", banTam.sdtKhachHang());
    assertEquals(2, banTam.dongHang().size());
    assertEquals(DSP_1, banTam.dongHang().get(0).maDongSP());
    assertEquals(3, banTam.dongHang().get(0).soLuong());
    assertEquals(DSP_2, banTam.dongHang().get(1).maDongSP());
    assertEquals(1, banTam.dongHang().get(1).soLuong());

    controller.xoaGio();
    assertNull(controller.maTamHienTai());
    assertTrue(controller.hoaDonHienTai().dongHang().isEmpty());

    sanPhamDao.xoaMay("SP-2026-000003");
    sanPhamDao.doiDong(DSP_2, dong(DSP_2, "Tủ lạnh", 2000000, "5", false));

    BanHangController.KetQuaKhoiPhuc khoiPhuc = controller.khoiPhucBanTam(banTam.maTam());

    assertEquals(banTam.maTam(), controller.maTamHienTai());
    assertEquals(List.of(DSP_1, DSP_2), khoiPhuc.dongDaDieuChinh());
    assertEquals(1, khoiPhuc.hoaDon().dongHang().size());
    assertEquals(DSP_1, khoiPhuc.hoaDon().dongHang().getFirst().maDongSP());
    assertEquals(2, khoiPhuc.hoaDon().dongHang().getFirst().soLuong(),
        "số lượng phải giảm theo tồn kho hiện tại");
    assertTien(2000010, khoiPhuc.hoaDon().tongTruocThue());
    assertTien(200002, khoiPhuc.hoaDon().tongThue());
    assertTien(2200012, khoiPhuc.hoaDon().tongThanhToan());
    assertTrue(hoaDonTamDao.findById(banTam.maTam()).isEmpty());
    assertTrue(controller.danhSachBanTam("NV-2026-000002").isEmpty());
  }

  @Test
  @DisplayName("Khôi phục xong hóa đơn tạm biến mất khỏi danh sách và lưu lại dùng đúng mã cũ")
  void khoiPhucXongBanTamBiXoaKhoiDanhSach() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    FakeHoaDonTamDao hoaDonTamDao = new FakeHoaDonTamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    DongSanPham tuLanh = dong(DSP_2, "Tủ lạnh", 2000000, "5", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000002", tuLanh, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = new BanHangController(sanPhamDao, hoaDonTamDao);
    controller.themDong(DSP_1, 1);
    controller.themDong(DSP_2, 1);
    HoaDonTamDao.HoaDonTam banTam = controller.luuBanTam("NV-2026-000002", "Nguyễn Văn A", null);
    String maTam = banTam.maTam();
    controller.xoaGio();

    controller.khoiPhucBanTam(maTam);

    assertTrue(hoaDonTamDao.findById(maTam).isEmpty());
    assertEquals(0, hoaDonTamDao.soBanTam());
    assertTrue(controller.danhSachBanTam("NV-2026-000002").isEmpty());
    assertEquals(2, controller.hoaDonHienTai().dongHang().size());

    HoaDonTamDao.HoaDonTam luuLai = controller.luuBanTam("NV-2026-000002", "Nguyễn Văn A", null);

    assertEquals(maTam, luuLai.maTam());
    assertEquals(maTam, controller.maTamHienTai());
    assertEquals(1, hoaDonTamDao.soBanTam());
    assertEquals(1, controller.danhSachBanTam("NV-2026-000002").size());
    assertTrue(hoaDonTamDao.findById(maTam).isPresent());
  }

  @Test
  @DisplayName("khoiPhucBanTam: mã tạm không tồn tại phải báo lỗi và không xóa hóa đơn tạm khác")
  void khoiPhucMaTamKhongTonTai() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    FakeHoaDonTamDao hoaDonTamDao = new FakeHoaDonTamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = new BanHangController(sanPhamDao, hoaDonTamDao);
    controller.themDong(DSP_1, 1);
    HoaDonTamDao.HoaDonTam daLuu = controller.luuBanTam("NV-2026-000002", "Nguyễn Văn A", null);

    IllegalArgumentException loi = assertThrows(IllegalArgumentException.class,
        () -> controller.khoiPhucBanTam("HDT-2026-999999"));

    assertTrue(loi.getMessage().contains("HDT-2026-999999"));
    assertEquals(1, hoaDonTamDao.soBanTam());
    assertTrue(hoaDonTamDao.findById(daLuu.maTam()).isPresent());
    assertEquals(1, controller.danhSachBanTam("NV-2026-000002").size());
  }

  @Test
  @DisplayName("hoaDonHienTai: áp khuyến mãi đang hiệu lực cho từng dòng sản phẩm")
  void khuyenMaiApChoDong() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000002", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    FakeKhuyenMaiDao khuyenMaiDao = new FakeKhuyenMaiDao();
    khuyenMaiDao.them(khuyenMai("KM-2026-000001", "10", true, mayGiat));
    BanHangController controller = controllerDayDu(sanPhamDao, khuyenMaiDao,
        new FakeHoaDonDao(), new FakeChiTietHoaDonDao());
    controller.themDong(DSP_1, 2);

    BanHangController.HoaDonTinh hoaDon = controller.hoaDonHienTai();

    assertEquals(1, hoaDon.dongHang().size());
    BanHangController.DongGio dongGio = hoaDon.dongHang().getFirst();
    assertNotNull(dongGio.khuyenMai(), "dòng phải giữ chương trình khuyến mãi đã áp");
    assertTien(10, dongGio.tyLeGiam());
    assertTien(200000, dongGio.tienKhuyenMai());
    assertTien(200000, hoaDon.tongKhuyenMai());
    assertTien(1800000, hoaDon.tongTruocThue());
    assertTien(180000, hoaDon.tongThue());
    assertTien(1980000, hoaDon.tongThanhToan());
  }

  @Test
  @DisplayName("hoaDonHienTai: không có khuyến mãi hiệu lực thì tiền giảm bằng 0")
  void khongCoKhuyenMaiThiGiamBangKhong() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000002", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = controllerDayDu(sanPhamDao, new FakeKhuyenMaiDao(),
        new FakeHoaDonDao(), new FakeChiTietHoaDonDao());
    controller.themDong(DSP_1, 2);

    BanHangController.HoaDonTinh hoaDon = controller.hoaDonHienTai();

    BanHangController.DongGio dongGio = hoaDon.dongHang().getFirst();
    assertNull(dongGio.khuyenMai());
    assertTien(0, dongGio.tienKhuyenMai());
    assertTien(0, hoaDon.tongKhuyenMai());
    assertTien(2000000, hoaDon.tongTruocThue());
    assertTien(200000, hoaDon.tongThue());
    assertTien(2200000, hoaDon.tongThanhToan());
  }

  @Test
  @DisplayName("thanhToan: từ chối khi tiền khách đưa chưa đủ và giữ nguyên giỏ hàng")
  void thanhToanThieuTienBiTuChoi() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    FakeHoaDonDao hoaDonDao = new FakeHoaDonDao();
    BanHangController controller = controllerDayDu(sanPhamDao, new FakeKhuyenMaiDao(),
        hoaDonDao, new FakeChiTietHoaDonDao());
    controller.themDong(DSP_1, 1);

    BanHangController.KetQuaThanhToan ketQua =
        controller.thanhToan(nhanVien(), khachHang(), 100000);

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("chưa đủ"));
    assertEquals(0, hoaDonDao.soLanGoi(), "không được ghi hóa đơn khi thiếu tiền");
    assertEquals(1, controller.hoaDonHienTai().dongHang().size(), "giỏ hàng phải được giữ nguyên");
  }

  @Test
  @DisplayName("thanhToan: máy vừa bị bán thì trả về thất bại và giữ nguyên giỏ hàng")
  void thanhToanMayVuaBanBiTuChoi() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    sanPhamDao.tuChoiDanhDau = true;
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    FakeHoaDonDao hoaDonDao = new FakeHoaDonDao();
    BanHangController controller = controllerDayDu(sanPhamDao, new FakeKhuyenMaiDao(),
        hoaDonDao, new FakeChiTietHoaDonDao());
    controller.themDong(DSP_1, 1);

    BanHangController.KetQuaThanhToan ketQua =
        controller.thanhToan(nhanVien(), khachHang(), 5000000);

    assertFalse(ketQua.thanhCong());
    assertTrue(ketQua.thongBao().contains("vừa được bán"));
    assertEquals(1, hoaDonDao.soLanGoi());
    assertEquals(1, controller.hoaDonHienTai().dongHang().size(), "giỏ hàng phải được giữ nguyên");
  }

  @Test
  @DisplayName("thanhToan: lỗi khi ghi chi tiết không làm mất giỏ hàng")
  void loiKhiGhiChiTietKhongLamMatGio() {
    FakeSanPhamDao sanPhamDao = new FakeSanPhamDao();
    DongSanPham mayGiat = dong(DSP_1, "Máy giặt", 1000000, "10", true);
    sanPhamDao.them(may("SP-2026-000001", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    sanPhamDao.them(may("SP-2026-000002", mayGiat, TrangThaiSanPham.SAN_SANG_BAN));
    BanHangController controller = controllerDayDu(sanPhamDao, new FakeKhuyenMaiDao(),
        new FakeHoaDonDao(), new FakeChiTietHoaDonDao(2));
    controller.themDong(DSP_1, 2);

    assertThrows(DataAccessException.class,
        () -> controller.thanhToan(nhanVien(), khachHang(), 5000000));

    BanHangController.HoaDonTinh conLai = controller.hoaDonHienTai();
    assertEquals(1, conLai.dongHang().size(), "giỏ hàng phải được giữ nguyên");
    assertEquals(2, conLai.dongHang().getFirst().soLuong());
  }

  private static BanHangController controllerDayDu(
      FakeSanPhamDao sanPhamDao,
      FakeKhuyenMaiDao khuyenMaiDao,
      FakeHoaDonDao hoaDonDao,
      FakeChiTietHoaDonDao chiTietHoaDonDao) {
    return new BanHangController(
        sanPhamDao,
        khuyenMaiDao,
        hoaDonDao,
        chiTietHoaDonDao,
        new FakeHoaDonTamDao(),
        dataSourceGia(),
        Clock.fixed(
            LocalDateTime.of(2026, 6, 1, 9, 0).atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()));
  }

  /**
   * Nguồn dữ liệu giả chỉ đủ để {@link com.dtaq.retail.util.MaGenerator} sinh mã và để bộ điều khiển
   * điều khiển giao dịch; không truy vấn cơ sở dữ liệu thật.
   */
  private static javax.sql.DataSource dataSourceGia() {
    ClassLoader lop = BanHangControllerTest.class.getClassLoader();
    java.sql.Connection ketNoi = (java.sql.Connection) java.lang.reflect.Proxy.newProxyInstance(
        lop,
        new Class<?>[] {java.sql.Connection.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getAutoCommit" -> true;
          case "isClosed" -> false;
          case "prepareStatement" -> preparedStatementGia();
          case "toString" -> "FakeConnection";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> null;
        });
    return (javax.sql.DataSource) java.lang.reflect.Proxy.newProxyInstance(
        lop,
        new Class<?>[] {javax.sql.DataSource.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "getConnection" -> ketNoi;
          case "toString" -> "FakeDataSource";
          case "hashCode" -> System.identityHashCode(proxy);
          case "equals" -> proxy == args[0];
          default -> null;
        });
  }

  private static java.sql.PreparedStatement preparedStatementGia() {
    ClassLoader lop = BanHangControllerTest.class.getClassLoader();
    java.sql.ResultSet ketQua = (java.sql.ResultSet) java.lang.reflect.Proxy.newProxyInstance(
        lop,
        new Class<?>[] {java.sql.ResultSet.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "next" -> true;
          case "getLong" -> 0L;
          case "toString" -> "FakeResultSet";
          default -> null;
        });
    return (java.sql.PreparedStatement) java.lang.reflect.Proxy.newProxyInstance(
        lop,
        new Class<?>[] {java.sql.PreparedStatement.class},
        (proxy, method, args) -> switch (method.getName()) {
          case "executeQuery" -> ketQua;
          case "executeUpdate" -> 1;
          case "toString" -> "FakePreparedStatement";
          default -> null;
        });
  }

  private static NhanVien nhanVien() {
    return new NhanVien(
        "NVBANHANG-2026-001", "Nguyễn Văn A", "0901234567", "a@dtaq.vn",
        LocalDate.of(2026, 1, 1), "nva", "hash", LoaiQuyen.NHAN_VIEN_BAN_HANG, true);
  }

  private static KhachHang khachHang() {
    return new KhachHang("KH-2026-000001", "Trần Thị B", "0907654321", "Cần Thơ");
  }

  private static KhuyenMai khuyenMai(
      String maKM, String tyLeGiam, boolean dangApDung, DongSanPham dongSanPham) {
    return new KhuyenMai(
        maKM,
        "Khuyến mãi " + tyLeGiam + "%",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        new BigDecimal(tyLeGiam),
        "Giảm " + tyLeGiam + "%",
        dangApDung,
        dongSanPham);
  }

  private static void assertTien(long mongDoi, BigDecimal thucTe) {
    assertNotNull(thucTe);
    assertEquals(0, BigDecimal.valueOf(mongDoi).compareTo(thucTe),
        "Mong đợi " + mongDoi + " nhưng nhận " + thucTe);
  }

  private static DongSanPham dong(
      String maDongSP, String tenDongSP, long donGia, String thue, boolean dangKinhDoanh) {
    return new DongSanPham(
        maDongSP,
        tenDongSP,
        DANH_MUC,
        "Máy",
        0,
        new BigDecimal(thue),
        BigDecimal.valueOf(donGia),
        dangKinhDoanh,
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 1, 1));
  }

  private static SanPham may(String soSerial, DongSanPham dongSanPham, TrangThaiSanPham trangThai) {
    return may(soSerial, dongSanPham, trangThai, LocalDate.of(2026, 1, 1));
  }

  private static SanPham may(
      String soSerial, DongSanPham dongSanPham, TrangThaiSanPham trangThai, LocalDate ngayNhap) {
    return new SanPham(
        soSerial,
        NHA_CUNG_CAP,
        dongSanPham,
        BigDecimal.valueOf(8000000),
        ngayNhap,
        trangThai,
        0,
        0);
  }

  /** DAO sản phẩm trong bộ nhớ, không truy cập cơ sở dữ liệu. */
  private static final class FakeSanPhamDao implements SanPhamDao {

    private final List<SanPham> kho = new ArrayList<>();
    private final Map<String, Boolean> daBan = new HashMap<>();
    boolean tuChoiDanhDau;
    private int soLanDanhDau;

    void them(SanPham sanPham) {
      kho.add(sanPham);
    }

    void xoaMay(String soSerial) {
      kho.removeIf(sanPham -> sanPham.soSerial().equals(soSerial));
    }

    void doiDong(String maDongSP, DongSanPham dongMoi) {
      for (int i = 0; i < kho.size(); i++) {
        SanPham cu = kho.get(i);
        if (cu.dongSanPham() != null && maDongSP.equals(cu.dongSanPham().maDongSP())) {
          kho.set(i, new SanPham(cu.soSerial(), cu.nhaCungCap(), dongMoi, cu.giaNhap(),
              cu.ngayNhapKhoBanDau(), cu.trangThai(), cu.soLanDoiTra(), cu.soLanSua()));
        }
      }
    }

    @Override
    public void insert(Connection ketNoi, SanPham sanPham) {
      kho.add(sanPham);
    }

    @Override
    public Optional<SanPham> findById(String soSerial) {
      return kho.stream().filter(sp -> sp.soSerial().equals(soSerial)).findFirst();
    }

    @Override
    public List<SanPham> findByDongSanPham(String maDongSP) {
      return kho.stream()
          .filter(sp -> sp.dongSanPham() != null && maDongSP.equals(sp.dongSanPham().maDongSP()))
          .toList();
    }

    @Override
    public List<SanPham> findByTrangThai(TrangThaiSanPham trangThai) {
      return kho.stream().filter(sp -> sp.trangThai() == trangThai).toList();
    }

    @Override
    public List<SanPham> findAll() {
      return List.copyOf(kho);
    }

    @Override
    public void updateTrangThai(String soSerial, TrangThaiSanPham trangThai) {
      for (int i = 0; i < kho.size(); i++) {
        SanPham cu = kho.get(i);
        if (cu.soSerial().equals(soSerial)) {
          kho.set(i, new SanPham(cu.soSerial(), cu.nhaCungCap(), cu.dongSanPham(), cu.giaNhap(),
              cu.ngayNhapKhoBanDau(), trangThai, cu.soLanDoiTra(), cu.soLanSua()));
        }
      }
    }

    @Override
    public boolean danhDauDaBan(Connection ketNoi, String soSerial) {
      soLanDanhDau++;
      if (tuChoiDanhDau) {
        return false;
      }
      daBan.put(soSerial, true);
      updateTrangThai(soSerial, TrangThaiSanPham.DA_BAN);
      return true;
    }

    boolean daBan(String soSerial) {
      return Boolean.TRUE.equals(daBan.get(soSerial));
    }

    int soLanDanhDau() {
      return soLanDanhDau;
    }
  }

  /** DAO khuyến mãi trong bộ nhớ, chỉ hỗ trợ tra cứu hiệu lực. */
  private static final class FakeKhuyenMaiDao implements KhuyenMaiDao {

    private final Map<String, KhuyenMai> theoDong = new LinkedHashMap<>();

    void them(KhuyenMai khuyenMai) {
      theoDong.put(khuyenMai.dongSanPham().maDongSP(), khuyenMai);
    }

    @Override
    public Optional<KhuyenMai> findHieuLuc(String maDongSP, LocalDate ngay) {
      KhuyenMai khuyenMai = maDongSP == null ? null : theoDong.get(maDongSP);
      if (khuyenMai == null || !khuyenMai.dangApDung()) {
        return Optional.empty();
      }
      if (ngay.isBefore(khuyenMai.ngayBatDau()) || ngay.isAfter(khuyenMai.ngayKetThuc())) {
        return Optional.empty();
      }
      return Optional.of(khuyenMai);
    }

    @Override
    public Optional<KhuyenMai> findById(String maKM) {
      return theoDong.values().stream()
          .filter(khuyenMai -> khuyenMai.maKM().equals(maKM))
          .findFirst();
    }

    @Override
    public List<KhuyenMai> findAll() {
      return List.copyOf(theoDong.values());
    }

    @Override
    public List<KhuyenMai> findByDongSanPham(String maDongSP) {
      KhuyenMai khuyenMai = theoDong.get(maDongSP);
      return khuyenMai == null ? List.of() : List.of(khuyenMai);
    }

    @Override
    public List<DongApDung> findDongTheoDieuKien(
        String maDongSP, List<String> maDanhMuc, LocalDate ngayNhapMuonNhat) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }

    @Override
    public void insert(KhuyenMai khuyenMai) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }

    @Override
    public void insert(Connection ketNoi, KhuyenMai khuyenMai) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }

    @Override
    public void update(KhuyenMai khuyenMai) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }

    @Override
    public void updateTrangThaiApDung(String maKM, boolean dangApDung) {
      throw new UnsupportedOperationException("Không dùng trong kiểm thử");
    }

    @Override
    public boolean existsTrungMa(String maKM) {
      return false;
    }

    @Override
    public boolean existsChongLan(
        String maDongSP, LocalDate ngayBatDau, LocalDate ngayKetThuc, String excludeMaKM) {
      return false;
    }
  }

  /** DAO hóa đơn trong bộ nhớ, đếm số lần ghi để kiểm tra giao dịch. */
  private static final class FakeHoaDonDao implements HoaDonDao {

    private final List<HoaDon> kho = new ArrayList<>();
    private int soLanGoi;

    @Override
    public void insert(Connection ketNoi, HoaDon hoaDon) {
      soLanGoi++;
      kho.add(hoaDon);
    }

    @Override
    public Optional<HoaDon> findById(String maHD) {
      return kho.stream().filter(hoaDon -> hoaDon.maHD().equals(maHD)).findFirst();
    }

    @Override
    public List<HoaDon> findDaThanhToanTrongKy(LocalDate tuNgay, LocalDate denNgay) {
      return List.of();
    }

    int soLanGoi() {
      return soLanGoi;
    }
  }

  /** DAO chi tiết hóa đơn trong bộ nhớ, có thể giả lập lỗi ở lần ghi thứ N. */
  private static final class FakeChiTietHoaDonDao implements ChiTietHoaDonDao {

    private final List<ChiTietHoaDon> kho = new ArrayList<>();
    private final int loiTaiLanThu;

    FakeChiTietHoaDonDao() {
      this(0);
    }

    FakeChiTietHoaDonDao(int loiTaiLanThu) {
      this.loiTaiLanThu = loiTaiLanThu;
    }

    @Override
    public void insert(Connection ketNoi, ChiTietHoaDon chiTiet) {
      if (loiTaiLanThu > 0 && kho.size() + 1 == loiTaiLanThu) {
        throw new DataAccessException("Lỗi ghi chi tiết giả lập");
      }
      kho.add(chiTiet);
    }

    @Override
    public List<ChiTietHoaDon> findByHoaDon(String maHD) {
      return kho.stream()
          .filter(chiTiet -> chiTiet.hoaDon().maHD().equals(maHD))
          .toList();
    }

    int soDong() {
      return kho.size();
    }
  }

  /** DAO hóa đơn tạm trong bộ nhớ với mã tự tăng theo quy ước HDT-2026-NNNNNN. */
  private static final class FakeHoaDonTamDao implements HoaDonTamDao {

    private final Map<String, HoaDonTam> kho = new LinkedHashMap<>();
    private int soThuTu;

    @Override
    public HoaDonTam luu(HoaDonTam banTam) {
      String maTam = banTam.maTam() == null || banTam.maTam().isBlank()
          ? String.format("HDT-2026-%06d", ++soThuTu)
          : banTam.maTam();
      HoaDonTam daLuu = new HoaDonTam(
          maTam,
          banTam.maNV(),
          banTam.tenNhanVien(),
          banTam.maKH(),
          banTam.tenKhachHang(),
          banTam.sdtKhachHang(),
          LocalDateTime.of(2026, 11, 3, 10, 0).plusMinutes(soThuTu),
          List.copyOf(banTam.dongHang()));
      kho.put(maTam, daLuu);
      return daLuu;
    }

    @Override
    public List<HoaDonTam> findByNhanVien(String maNV) {
      return kho.values().stream()
          .filter(hoaDon -> hoaDon.maNV().equals(maNV))
          .sorted(Comparator.comparing(HoaDonTam::luuLuc).reversed())
          .toList();
    }

    @Override
    public Optional<HoaDonTam> findById(String maTam) {
      return Optional.ofNullable(kho.get(maTam));
    }

    @Override
    public void xoa(String maTam) {
      kho.remove(maTam);
    }

    int soBanTam() {
      return kho.size();
    }
  }
}
