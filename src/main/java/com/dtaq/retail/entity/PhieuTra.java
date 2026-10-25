package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Phiếu trả hàng, ánh xạ bảng "PhieuTra". */
public record PhieuTra(
    String maPhieuTra,
    NhanVien nhanVienLap,
    HoaDon hoaDon,
    LocalDate ngayTao,
    LocalDate ngayHoanTat,
    TrangThaiDuyet trangThaiDuyet,
    TrangThaiXuLyPhieu trangThaiXuLyPhieu,
    NhanVien nhanVienDuyet,
    LocalDate ngayDuyet,
    String lyDoDuyet,
    BigDecimal tienKhachDua) {
}
