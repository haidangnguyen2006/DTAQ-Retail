package com.dtaq.retail.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Số liệu chỉ đọc của một ngày cho trang chủ Bán hàng và trang chủ Quản lý. */
public interface TrangChuDao {

  /**
   * Tổng hợp hóa đơn đã thanh toán trong ngày.
   *
   * @param soHoaDon số hóa đơn đã thanh toán đủ
   * @param tongThu tổng tiền khách trả (sau khuyến mãi, gồm thuế)
   * @param tienThue phần thuế trong {@code tongThu}
   * @param soKhach số khách hàng khác nhau có hóa đơn
   */
  record TongQuanBanHang(long soHoaDon, long tongThu, long tienThue, long soKhach) {}

  /** Một hóa đơn trong ngày, tổng thanh toán cộng từ các dòng chi tiết đã làm tròn. */
  record HoaDonGanDay(String maHD, String tenKhachHang, long tongThanhToan, boolean daThanhToan) {}

  /** Số phiếu đổi/trả trong ngày. */
  record DoiTraTrongNgay(long soHoanTat, long soDaDuyet, long soTuChoi) {}

  /** Một lần sao lưu/phục hồi trong ngày. */
  record SaoLuuTrongNgay(LocalDateTime thoiDiem, String loai, boolean thanhCong) {}

  /**
   * @param ngay ngày lập hóa đơn
   * @param maNV chỉ tính hóa đơn của nhân viên này; {@code null} để tính toàn cửa hàng
   */
  TongQuanBanHang tongQuanBanHang(LocalDate ngay, String maNV);

  /**
   * Các hóa đơn mới nhất trong ngày, mã giảm dần.
   *
   * @param maNV chỉ lấy hóa đơn của nhân viên này; {@code null} để lấy toàn cửa hàng
   * @param gioiHan số dòng tối đa, phải lớn hơn 0
   */
  List<HoaDonGanDay> hoaDonGanDay(LocalDate ngay, String maNV, int gioiHan);

  /** Phiếu trả + phiếu đổi hoàn tất, được duyệt, bị từ chối trong ngày. */
  DoiTraTrongNgay doiTraTrongNgay(LocalDate ngay);

  /** Các lần sao lưu/phục hồi trong ngày, mới nhất trước. */
  List<SaoLuuTrongNgay> saoLuuTrongNgay(LocalDate ngay);

  /** Số máy còn tồn có số ngày lưu kho tính đến {@code ngay} đạt {@code nguongNgay}. */
  long soMayTonLau(LocalDate ngay, int nguongNgay);
}
