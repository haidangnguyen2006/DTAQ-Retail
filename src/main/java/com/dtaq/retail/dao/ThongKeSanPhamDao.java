package com.dtaq.retail.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Truy vấn thống kê doanh số theo sản phẩm cho UC015. */
public interface ThongKeSanPhamDao {

  /** Một dòng thống kê doanh số của một dòng sản phẩm trong kỳ. */
  record DongThongKe(
      String maDongSP,
      String tenDongSP,
      String tenDanhMuc,
      int soLuongTon,
      long soLuongBan,
      BigDecimal tienTruocThue,
      long soLuongTra,
      BigDecimal doanhSoSauDieuChinh) {}

  /**
   * Tính doanh số theo sản phẩm trong một kỳ cho một dòng sản phẩm cụ thể.
   *
   * @param tuNgay ngày bắt đầu kỳ, không được {@code null}
   * @param denNgay ngày kết thúc kỳ, không được {@code null}
   * @param maDongSP mã dòng sản phẩm cần lọc; {@code null} hoặc rỗng để lấy tất cả
   * @return danh sách doanh số đã tổng hợp, sắp xếp theo mã dòng sản phẩm
   */
  List<DongThongKe> thongKe(LocalDate tuNgay, LocalDate denNgay, String maDongSP);

  /**
   * Một giao dịch dùng để tổng hợp dòng sản phẩm (UC015 9.a): một máy bán ({@code laTra = false},
   * giá trị dương) hoặc một máy trả hoàn tất ({@code laTra = true}, giá trị âm).
   */
  record GiaoDichDoiChieu(
      boolean laTra, String maChungTu, String maHD, LocalDate ngay, String soSerial, BigDecimal giaTri) {}

  /**
   * Liệt kê các giao dịch đã dùng để tính dòng {@code maDongSP} trong kỳ, cùng điều kiện với
   * {@link #thongKe}: tổng {@code giaTri} bằng doanh số sau điều chỉnh, số dòng bán/trả bằng số lượng
   * bán/trả.
   */
  List<GiaoDichDoiChieu> doiChieu(LocalDate tuNgay, LocalDate denNgay, String maDongSP);
}
