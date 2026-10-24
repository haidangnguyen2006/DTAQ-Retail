package com.dtaq.retail.entity;

import java.time.LocalDate;

/** Bản ghi sản phẩm lỗi, ánh xạ bảng "SanPhamLoi". */
public record SanPhamLoi(
    String maLoi,
    SanPham sanPham,
    PhieuTra phieuTra,
    PhieuDoi phieuDoi,
    String ghiChuXuLy,
    String moTaLoi,
    LocalDate ngayGhiNhan,
    TrangThaiXuLyLoi tinhTrangXuLy,
    LocalDate ngayCapNhap) {
}
