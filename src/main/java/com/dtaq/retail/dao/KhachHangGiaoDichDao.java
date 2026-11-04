package com.dtaq.retail.dao;

import com.dtaq.retail.entity.TinhTrangHangTra;
import com.dtaq.retail.entity.TrangThaiDuyet;
import com.dtaq.retail.entity.TrangThaiXuLyPhieu;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Truy vấn chỉ đọc lịch sử giao dịch của khách hàng (UC005): hóa đơn, chi tiết hóa đơn gốc, phiếu
 * đổi/trả và chi tiết phiếu. Mọi số tiền lấy từ giá trị đã chốt trên chi tiết hóa đơn tại thời điểm
 * bán, không tính lại theo giá hay thuế hiện tại.
 */
public interface KhachHangGiaoDichDao {

  /**
   * Liệt kê hóa đơn của khách hàng, mới nhất trước.
   *
   * @param maKH mã khách hàng
   * @return danh sách hóa đơn, có thể rỗng
   */
  List<HoaDonTomTat> hoaDonCuaKhach(String maKH);

  /**
   * Liệt kê chi tiết của một hóa đơn gốc, sắp xếp theo số serial.
   *
   * @param maHD mã hóa đơn
   * @return các dòng chi tiết, có thể rỗng
   */
  List<ChiTietHoaDonView> chiTietHoaDon(String maHD);

  /**
   * Liệt kê phiếu trả và phiếu đổi lập trên các hóa đơn của khách hàng, mới nhất trước.
   *
   * @param maKH mã khách hàng
   * @return danh sách phiếu, có thể rỗng
   */
  List<PhieuTomTat> phieuCuaKhach(String maKH);

  /**
   * Liệt kê các dòng hàng của một phiếu đổi hoặc phiếu trả.
   *
   * @param loai loại phiếu
   * @param maPhieu mã phiếu
   * @return các dòng của phiếu, có thể rỗng
   */
  List<ChiTietPhieuView> chiTietPhieu(LoaiPhieu loai, String maPhieu);

  /** Loại phiếu liên quan tới hóa đơn. */
  enum LoaiPhieu {
    TRA,
    DOI
  }

  /**
   * Tóm tắt một hóa đơn trong lịch sử mua hàng.
   *
   * @param maHD mã hóa đơn
   * @param ngayLap ngày lập
   * @param tenNhanVienLap họ tên nhân viên lập hóa đơn
   * @param soSanPham số dòng sản phẩm (máy) trên hóa đơn
   * @param tienHang tổng tiền sau khuyến mãi, trước thuế
   * @param tienThue tổng tiền thuế
   * @param tongThanhToan tổng tiền hàng cộng thuế
   * @param daThanhToan hóa đơn đã thanh toán hay chưa
   */
  record HoaDonTomTat(String maHD, LocalDate ngayLap, String tenNhanVienLap, int soSanPham,
      BigDecimal tienHang, BigDecimal tienThue, BigDecimal tongThanhToan, boolean daThanhToan) {
  }

  /**
   * Một dòng chi tiết của hóa đơn gốc, theo giá trị đã chốt lúc bán.
   *
   * @param soSerial số serial của máy
   * @param maDongSP mã dòng sản phẩm
   * @param tenDongSP tên dòng sản phẩm
   * @param donGia đơn giá bán trước thuế tại lúc bán
   * @param thueSuat thuế suất (%) tại lúc bán
   * @param tienKhuyenMai tiền khuyến mãi thực tế
   * @param tienSauKhuyenMai tiền sau khuyến mãi, trước thuế
   * @param tienThue tiền thuế
   * @param thanhTien tiền sau khuyến mãi cộng thuế
   */
  record ChiTietHoaDonView(String soSerial, String maDongSP, String tenDongSP, BigDecimal donGia,
      BigDecimal thueSuat, BigDecimal tienKhuyenMai, BigDecimal tienSauKhuyenMai,
      BigDecimal tienThue, BigDecimal thanhTien) {
  }

  /**
   * Tóm tắt một phiếu đổi hoặc phiếu trả.
   *
   * @param loai loại phiếu
   * @param maPhieu mã phiếu
   * @param maHD mã hóa đơn gốc
   * @param ngayTao ngày tạo phiếu
   * @param ngayHoanTat ngày hoàn tất; {@code null} nếu chưa hoàn tất
   * @param trangThaiDuyet trạng thái duyệt
   * @param trangThaiXuLy trạng thái xử lý
   * @param lyDoDuyet ý kiến hoặc lý do của người duyệt; có thể {@code null}
   */
  record PhieuTomTat(LoaiPhieu loai, String maPhieu, String maHD, LocalDate ngayTao,
      LocalDate ngayHoanTat, TrangThaiDuyet trangThaiDuyet, TrangThaiXuLyPhieu trangThaiXuLy,
      String lyDoDuyet) {
  }

  /**
   * Một dòng hàng của phiếu đổi hoặc phiếu trả.
   *
   * @param soSerial số serial máy trên hóa đơn gốc
   * @param tenDongSP tên dòng sản phẩm của máy
   * @param lyDo lý do trả hoặc đổi
   * @param tinhTrangHangTra tình trạng hàng trả; {@code null} với phiếu đổi
   * @param serialThayThe serial máy thay thế; {@code null} với phiếu trả
   */
  record ChiTietPhieuView(String soSerial, String tenDongSP, String lyDo,
      TinhTrangHangTra tinhTrangHangTra, String serialThayThe) {
  }
}
