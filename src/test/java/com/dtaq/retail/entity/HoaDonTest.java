package com.dtaq.retail.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Kiểm thử các tổng tiền dẫn xuất của {@link HoaDon}. */
class HoaDonTest {

  private static ChiTietHoaDon chiTiet(String tienSauKhuyenMai, String tienThue) {
    return new ChiTietHoaDon(
        null,
        null,
        1,
        null,
        null,
        null,
        null,
        null,
        null,
        new BigDecimal(tienSauKhuyenMai),
        new BigDecimal(tienThue));
  }

  private static HoaDon hoaDonMau() {
    NhanVien nhanVien = new NhanVien(
        "NVBANHANG-2026-000001",
        "Nguyễn Văn Bán",
        "0901234567",
        "banhang@dtaq.vn",
        LocalDate.of(2026, 1, 1),
        "banhang",
        "matkhau-gia-lap",
        LoaiQuyen.NHAN_VIEN_BAN_HANG,
        true);
    KhachHang khachHang = new KhachHang(
        "KH-2026-000001",
        "Trần Thị Mua",
        "0909876543",
        "123 Lê Lợi, Quận 1");
    return new HoaDon("HD-2026-000999", LocalDate.of(2026, 1, 5), nhanVien, khachHang, true);
  }

  @Test
  @DisplayName("Tổng tiền trước thuế là tổng các giá trị đã lưu, không làm tròn lại")
  void tongTienTruocThueCongDonKhongLamTron() {
    List<ChiTietHoaDon> chiTiet = List.of(chiTiet("0.4", "0.4"), chiTiet("0.4", "0.6"));
    assertEquals(new BigDecimal("0.8"), hoaDonMau().tongTienTruocThue(chiTiet));
  }

  @Test
  @DisplayName("Tổng tiền thuế là tổng các giá trị thuế đã lưu của từng dòng")
  void tongTienThueCongDonCacDong() {
    List<ChiTietHoaDon> chiTiet = List.of(chiTiet("0.4", "0.4"), chiTiet("0.4", "0.6"));
    assertEquals(new BigDecimal("1.0"), hoaDonMau().tongTienThue(chiTiet));
  }

  @Test
  @DisplayName("Tổng tiền thanh toán bằng tổng trước thuế cộng tổng thuế")
  void tongTienThanhToanBangTongTruocThueCongThue() {
    List<ChiTietHoaDon> chiTiet = List.of(chiTiet("0.4", "0.4"), chiTiet("0.4", "0.6"));
    assertEquals(new BigDecimal("1.8"), hoaDonMau().tongTienThanhToan(chiTiet));
  }

  @Test
  @DisplayName("Danh sách chi tiết rỗng thì tổng tiền bằng không")
  void danhSachRongThiTongBangKhong() {
    assertEquals(BigDecimal.ZERO, hoaDonMau().tongTienTruocThue(Collections.emptyList()));
    assertEquals(BigDecimal.ZERO, hoaDonMau().tongTienThue(Collections.emptyList()));
    assertEquals(BigDecimal.ZERO, hoaDonMau().tongTienThanhToan(Collections.emptyList()));
  }

  @Test
  @DisplayName("Danh sách chi tiết null thì báo lỗi tham số")
  void danhSachNullThiBaoLoi() {
    assertThrows(IllegalArgumentException.class, () -> hoaDonMau().tongTienTruocThue(null));
    assertThrows(IllegalArgumentException.class, () -> hoaDonMau().tongTienThue(null));
    assertThrows(IllegalArgumentException.class, () -> hoaDonMau().tongTienThanhToan(null));
  }

  @Test
  @DisplayName("Chuỗi mô tả hóa đơn chứa mã hóa đơn, nhân viên, khách hàng và không duyệt chi tiết")
  void chuoiMoTaKhongDuyetChiTiet() {
    String moTa = hoaDonMau().toString();
    assertTrue(moTa.contains("HD-2026-000999"));
    assertTrue(moTa.contains("NVBANHANG-2026-000001"));
    assertTrue(moTa.contains("KH-2026-000001"));
    assertFalse(moTa.contains("SN-2026-000001"));
  }
}
