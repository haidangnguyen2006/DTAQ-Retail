package com.dtaq.retail.entity;

/**
 * Dòng chi tiết phiếu đổi, ánh xạ bảng "ChiTietPhieuDoi".
 *
 * <p>{@code tinhTrangHangTra} phản ánh tình trạng máy khách trả lại (UC006 bước 6): máy nguyên vẹn
 * được đưa lại về kho bán, máy lỗi chuyển sang nhóm cần xử lý (bước 15.a).
 */
public record ChiTietPhieuDoi(
    PhieuDoi phieuDoi,
    ChiTietHoaDon chiTietHoaDon,
    String lyDoDoi,
    TinhTrangHangTra tinhTrangHangTra,
    SanPham chiTietSanPhamDoi) {
}
