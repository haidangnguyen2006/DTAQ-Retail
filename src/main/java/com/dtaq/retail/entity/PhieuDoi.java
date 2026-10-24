package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Phiếu đổi hàng, ánh xạ bảng "PhieuDoi". */
public record PhieuDoi(
    String maPhieuDoi,
    NhanVien nhanVienLap,
    HoaDon hoaDon,
    LocalDate ngayHoanTat,
    LocalDate ngayTao,
    TrangThaiDuyet trangThaiDuyet,
    TrangThaiXuLyPhieu trangThaiXuLyPhieu,
    BigDecimal tienKhachDuaThem,
    NhanVien nhanVienDuyet,
    LocalDate ngayDuyet,
    String lyDoDuyet) {
}
