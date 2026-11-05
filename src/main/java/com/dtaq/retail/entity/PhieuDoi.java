package com.dtaq.retail.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Phiếu đổi hàng, ánh xạ bảng "PhieuDoi".
 *
 * <p>{@code tienKhachDuaThem} là phần khách trả thêm (luôn {@code >= 0}), còn
 * {@code tienChenhLech} là hiệu có dấu giữa giá trị máy mới và giá trị máy trả: âm nghĩa là cửa
 * hàng phải hoàn tiền, dương nghĩa là khách phải trả thêm, bằng 0 là đổi ngang.
 */
public record PhieuDoi(
    String maPhieuDoi,
    NhanVien nhanVienLap,
    HoaDon hoaDon,
    LocalDate ngayHoanTat,
    LocalDate ngayTao,
    TrangThaiDuyet trangThaiDuyet,
    TrangThaiXuLyPhieu trangThaiXuLyPhieu,
    BigDecimal tienKhachDuaThem,
    BigDecimal tienChenhLech,
    NhanVien nhanVienDuyet,
    LocalDate ngayDuyet,
    String lyDoDuyet) {
}
