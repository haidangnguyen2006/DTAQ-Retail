package com.dtaq.retail.entity;

/** Dòng chi tiết phiếu đổi, ánh xạ bảng "ChiTietPhieuDoi". */
public record ChiTietPhieuDoi(
    PhieuDoi phieuDoi,
    ChiTietHoaDon chiTietHoaDon,
    String lyDoDoi,
    SanPham chiTietSanPhamDoi) {
}
