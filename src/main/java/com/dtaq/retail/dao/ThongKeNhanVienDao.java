package com.dtaq.retail.dao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Truy vấn chỉ đọc cho báo cáo doanh số theo nhân viên (UC016).
 *
 * <p>Doanh số là tiền hàng trước thuế ({@code ChiTietHoaDon.tienSauKhuyenMai}) của hóa đơn đã thanh
 * toán, lập trong kỳ. Điều chỉnh là giá trị trước thuế của các máy trên phiếu trả đã hoàn tất trong
 * kỳ, lấy từ chi tiết hóa đơn gốc và quy về nhân viên lập hóa đơn gốc (RB65, XBR-11). Mọi số tiền
 * là giá trị đã làm tròn sẵn trong CSDL, chỉ được cộng/trừ, không làm tròn lại.
 */
public interface ThongKeNhanVienDao {

  /**
   * Tổng hợp theo nhân viên lập hóa đơn. Chỉ trả về nhân viên có phát sinh trong kỳ (bán hoặc bị
   * điều chỉnh), sắp xếp theo mã nhân viên.
   *
   * @param tuNgay ngày đầu kỳ (bao gồm)
   * @param denNgay ngày cuối kỳ (bao gồm)
   * @param maNV mã nhân viên cần xem; {@code null} để lấy tất cả
   * @return các dòng thống kê, có thể rỗng
   */
  List<DongThongKeNhanVien> thongKe(LocalDate tuNgay, LocalDate denNgay, String maNV);

  /**
   * Hóa đơn đã thanh toán của một nhân viên trong kỳ, dùng để đối chiếu doanh số (luồng 10.a).
   *
   * @param tuNgay ngày đầu kỳ (bao gồm)
   * @param denNgay ngày cuối kỳ (bao gồm)
   * @param maNV mã nhân viên lập hóa đơn
   * @return hóa đơn theo ngày lập tăng dần, có thể rỗng
   */
  List<HoaDonDoiChieu> hoaDonTrongKy(LocalDate tuNgay, LocalDate denNgay, String maNV);

  /**
   * Phiếu trả hoàn tất trong kỳ trên hóa đơn do nhân viên lập, dùng để đối chiếu điều chỉnh (10.a).
   *
   * @param tuNgay ngày đầu kỳ (bao gồm)
   * @param denNgay ngày cuối kỳ (bao gồm)
   * @param maNV mã nhân viên lập hóa đơn gốc
   * @return phiếu theo ngày hoàn tất tăng dần, có thể rỗng
   */
  List<DieuChinhDoiChieu> dieuChinhTrongKy(LocalDate tuNgay, LocalDate denNgay, String maNV);

  /**
   * Một dòng thống kê của một nhân viên.
   *
   * @param maNV mã nhân viên
   * @param hoTen họ tên nhân viên
   * @param dangHoatDong tài khoản còn hoạt động hay đã khóa
   * @param soHoaDon số hóa đơn đã thanh toán lập trong kỳ
   * @param doanhSo tổng tiền hàng trước thuế của các hóa đơn đó
   * @param dieuChinh giá trị trước thuế của hàng trả hoàn tất trong kỳ (số dương, bị trừ)
   */
  record DongThongKeNhanVien(String maNV, String hoTen, boolean dangHoatDong, int soHoaDon,
      BigDecimal doanhSo, BigDecimal dieuChinh) {

    /** Doanh số sau khi trừ điều chỉnh đổi/trả. */
    public BigDecimal sauDieuChinh() {
      return doanhSo.subtract(dieuChinh);
    }
  }

  /**
   * Hóa đơn dùng để tính doanh số.
   *
   * @param maHD mã hóa đơn
   * @param ngayLap ngày lập
   * @param tenKhachHang họ tên khách hàng
   * @param soSanPham số máy trên hóa đơn
   * @param tienTruocThue tiền hàng trước thuế
   */
  record HoaDonDoiChieu(String maHD, LocalDate ngayLap, String tenKhachHang, int soSanPham,
      BigDecimal tienTruocThue) {
  }

  /**
   * Phiếu trả dùng để điều chỉnh doanh số.
   *
   * @param maPhieuTra mã phiếu trả
   * @param maHD mã hóa đơn gốc
   * @param ngayHoanTat ngày hoàn tất phiếu
   * @param soMay số máy trả
   * @param giaTriTru giá trị trước thuế bị trừ, lấy từ chi tiết hóa đơn gốc
   */
  record DieuChinhDoiChieu(String maPhieuTra, String maHD, LocalDate ngayHoanTat, int soMay,
      BigDecimal giaTriTru) {
  }
}
