package com.dtaq.retail.entity;

/** Dòng chi tiết phiếu trả, ánh xạ bảng "ChiTietPhieuTra". */
public record ChiTietPhieuTra(
    PhieuTra phieuTra,
    ChiTietHoaDon chiTietHoaDon,
    TinhTrangHangTra tinhTrangHangTra,
    String lyDoTra) {
}
