package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Hóa đơn bán lẻ, ánh xạ bảng "HoaDon". */
public record HoaDon(
    String maHD,
    LocalDate ngayLap,
    NhanVien nhanVienLap,
    KhachHang khachHang,
    boolean trangThaiThanhToan) {

  private static final String CHI_TIET_TRONG =
      "Danh sách chi tiết không được để trống";

  /** Tổng tiền trước thuế, cộng các giá trị "tienSauKhuyenMai" đã lưu. */
  public BigDecimal tongTienTruocThue(List<ChiTietHoaDon> chiTiet) {
    kiemTraChiTiet(chiTiet);
    return chiTiet.stream()
        .map(ChiTietHoaDon::tienSauKhuyenMai)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Tổng tiền thuế, cộng các giá trị "tienThue" đã lưu. */
  public BigDecimal tongTienThue(List<ChiTietHoaDon> chiTiet) {
    kiemTraChiTiet(chiTiet);
    return chiTiet.stream()
        .map(ChiTietHoaDon::tienThue)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Tổng tiền thanh toán, bằng tổng tiền trước thuế cộng tổng tiền thuế. */
  public BigDecimal tongTienThanhToan(List<ChiTietHoaDon> chiTiet) {
    return tongTienTruocThue(chiTiet).add(tongTienThue(chiTiet));
  }

  private static void kiemTraChiTiet(List<ChiTietHoaDon> chiTiet) {
    if (chiTiet == null) {
      throw new IllegalArgumentException(CHI_TIET_TRONG);
    }
  }

  /** Mô tả hóa đơn theo khóa ngoại, không duyệt danh sách chi tiết. */
  @Override
  public String toString() {
    return "HoaDon[maHD=" + maHD
        + ", ngayLap=" + ngayLap
        + ", nhanVienLap=" + (nhanVienLap == null ? null : nhanVienLap.maNV())
        + ", khachHang=" + (khachHang == null ? null : khachHang.maKH())
        + ", trangThaiThanhToan=" + trangThaiThanhToan
        + "]";
  }
}
