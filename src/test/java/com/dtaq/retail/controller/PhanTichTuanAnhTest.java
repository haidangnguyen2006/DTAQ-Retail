package com.dtaq.retail.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.controller.ThongKeNhanVienController.BaoCao;
import com.dtaq.retail.controller.ThongKeNhanVienController.MucNhanXet;
import com.dtaq.retail.controller.ThongKeNhanVienController.NhanXet;
import com.dtaq.retail.controller.TraCuuKhachHangController.TongQuanKhach;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.HoaDonTomTat;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.LoaiPhieu;
import com.dtaq.retail.dao.KhachHangGiaoDichDao.PhieuTomTat;
import com.dtaq.retail.dao.ThongKeNhanVienDao.DongThongKeNhanVien;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import com.dtaq.retail.util.DateRange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Tóm tắt khách hàng (UC005) và nhận xét doanh số nhân viên (UC016)")
class PhanTichTuanAnhTest {

  private static final LocalDate HOM_NAY = LocalDate.of(2026, 11, 6);

  private static HoaDonTomTat hd(String ma, LocalDate ngay, long tong, boolean daTra) {
    return new HoaDonTomTat(ma, ngay, "NV", 1, BigDecimal.valueOf(tong), BigDecimal.ZERO,
        BigDecimal.valueOf(tong), daTra);
  }

  @Test
  @DisplayName("Tổng quan khách: chỉ tính hóa đơn đã thanh toán, TB làm tròn HALF_UP, phiếu đang mở")
  void tongQuanKhach() {
    List<HoaDonTomTat> hoaDons = List.of(
        hd("HD1", LocalDate.of(2026, 11, 1), 1_000_001, true),
        hd("HD2", LocalDate.of(2026, 1, 10), 2_000_000, true),
        hd("HD3", LocalDate.of(2026, 11, 5), 9_000_000, false));
    List<PhieuTomTat> phieus = List.of(new PhieuTomTat(LoaiPhieu.TRA, "PT1", "HD1", HOM_NAY, null,
        TrangThaiDuyet.CHO_DUYET, TrangThaiXuLyPhieu.CHO_XU_LY, null));

    TongQuanKhach tq = TraCuuKhachHangController.tongQuan(hoaDons, phieus, HOM_NAY);

    assertEquals(2, tq.soHoaDon());
    assertEquals(new BigDecimal("3000001"), tq.tongChiTieu());
    assertEquals(new BigDecimal("1500001"), tq.trungBinhHoaDon());
    assertEquals(LocalDate.of(2026, 11, 1), tq.lanMuaGanNhat());
    assertEquals(LocalDate.of(2026, 1, 10), tq.lanMuaDauTien());
    assertEquals(5L, tq.soNgayTuLanCuoi());
    assertEquals(1, tq.soPhieuDangMo());

    TongQuanKhach trong = TraCuuKhachHangController.tongQuan(List.of(), List.of(), HOM_NAY);
    assertEquals(BigDecimal.ZERO, trong.trungBinhHoaDon());
    assertNull(trong.soNgayTuLanCuoi());
  }

  private static DongThongKeNhanVien dong(String ma, String ten, int soHd, long doanhSo, long dieuChinh) {
    return new DongThongKeNhanVien(ma, ten, true, soHd, BigDecimal.valueOf(doanhSo), BigDecimal.valueOf(dieuChinh));
  }

  @Test
  @DisplayName("Nhận xét: xu hướng, dẫn đầu phụ thuộc, tỉ lệ trả cao, giảm mạnh so với kỳ trước")
  void nhanXet() {
    DateRange ky = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
    DateRange truoc = new DateRange(LocalDate.of(2026, 8, 31), LocalDate.of(2026, 9, 30));
    BaoCao hienTai = new BaoCao(ky, List.of(
        dong("A", "An", 10, 80_000_000, 0),
        dong("B", "Bình", 4, 10_000_000, 3_000_000),
        dong("C", "Chi", 2, 10_000_000, 0)));
    BaoCao kyTruoc = new BaoCao(truoc, List.of(
        dong("A", "An", 8, 60_000_000, 0),
        dong("C", "Chi", 5, 30_000_000, 0)));

    List<NhanXet> ketQua = ThongKeNhanVienController.nhanXet(hienTai, kyTruoc);
    List<String> noiDung = ketQua.stream().map(NhanXet::noiDung).toList();

    // Tổng thực 97tr so với 90tr: tăng 7,8%.
    assertTrue(noiDung.get(0).startsWith("Doanh số thực tăng 7,8%"), noiDung.get(0));
    assertTrue(ketQua.stream().anyMatch(n -> n.muc() == MucNhanXet.CANH_BAO
        && n.noiDung().startsWith("An dẫn đầu") && n.noiDung().contains("phụ thuộc")), noiDung.toString());
    assertTrue(noiDung.stream().anyMatch(s -> s.startsWith("Bình có tỉ lệ hàng trả 30%")), noiDung.toString());
    assertTrue(noiDung.stream().anyMatch(s -> s.startsWith("Chi giảm 66,7%")), noiDung.toString());
    assertTrue(ThongKeNhanVienController.nhanXet(new BaoCao(ky, List.of()), kyTruoc).isEmpty());
  }

  @Test
  @DisplayName("Phần trăm định dạng kiểu Việt Nam, chia cho 0 trả về —")
  void phanTram() {
    assertEquals("12,5%", ThongKeNhanVienController.dinhDangPhanTram(
        ThongKeNhanVienController.phanTram(BigDecimal.ONE, BigDecimal.valueOf(8))));
    assertEquals("—", ThongKeNhanVienController.dinhDangPhanTram(
        ThongKeNhanVienController.phanTram(BigDecimal.ONE, BigDecimal.ZERO)));
    assertEquals(new BigDecimal("333334"),
        ThongKeNhanVienController.trungBinhHoaDon(BigDecimal.valueOf(1_000_001), 3));
  }
}
