package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Một máy cụ thể trong kho, ánh xạ bảng "SanPham". */
public record SanPham(
    String soSerial,
    NhaCungCap nhaCungCap,
    DongSanPham dongSanPham,
    BigDecimal giaNhap,
    LocalDate ngayNhapKhoBanDau,
    TrangThaiSanPham trangThai,
    int soLanDoiTra,
    int soLanSua) {
}
