package com.dtaq.retail.entity;

import java.math.BigDecimal;

/** Dòng chi tiết hóa đơn, ánh xạ bảng "ChiTietHoaDon". */
public record ChiTietHoaDon(
    HoaDon hoaDon,
    SanPham sanPham,
    int soLuong,
    BigDecimal donGiaBanTaiLucBan,
    BigDecimal thueSuatTaiLucBan,
    KhuyenMai khuyenMai,
    BigDecimal tyLeGiamTaiLucBan,
    BigDecimal tienKhuyenMaiThucTe,
    BigDecimal tienTruocKhuyenMai,
    BigDecimal tienSauKhuyenMai,
    BigDecimal tienThue) {

  /** Mô tả dòng chi tiết, không in lại hóa đơn cha. */
  @Override
  public String toString() {
    return "ChiTietHoaDon[maHD=" + (hoaDon == null ? null : hoaDon.maHD())
        + ", soSerial=" + (sanPham == null ? null : sanPham.soSerial())
        + ", soLuong=" + soLuong
        + ", tienSauKhuyenMai=" + tienSauKhuyenMai
        + "]";
  }
}
