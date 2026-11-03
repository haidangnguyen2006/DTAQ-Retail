package com.dtaq.retail.dao;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Lưu trữ hóa đơn tạm (hóa đơn đang lập, chưa thanh toán) của nhân viên bán hàng.
 *
 * <p>Hóa đơn tạm chỉ phục vụ việc tạm dừng và khôi phục thao tác lập hóa đơn; dữ liệu
 * không tham gia vào nghiệp vụ thanh toán và không thay thế hóa đơn chính thức.
 */
public interface HoaDonTamDao {

  /**
   * Một dòng hàng của hóa đơn tạm.
   *
   * <p>Theo UC003 bước 4, hóa đơn bán lẻ được lập theo <b>dòng sản phẩm và số lượng</b>; máy cụ thể
   * (số serial) chỉ được chọn ở bước thanh toán. Vì vậy dòng của hóa đơn tạm chỉ lưu mã dòng sản
   * phẩm cùng số lượng, không lưu số serial.
   *
   * @param maDongSP mã dòng sản phẩm
   * @param tenDongSP tên dòng sản phẩm
   * @param soLuong số lượng máy của dòng sản phẩm
   * @param donGiaTruocThue đơn giá bán trước thuế của một máy
   * @param thueSuat thuế suất áp dụng
   */
  record DongTam(
      String maDongSP, String tenDongSP, int soLuong,
      BigDecimal donGiaTruocThue, BigDecimal thueSuat) {}

  /**
   * Một hóa đơn tạm đã lưu.
   *
   * @param maTam mã hóa đơn tạm
   * @param maNV mã nhân viên đang lập hóa đơn
   * @param tenNhanVien tên nhân viên đang lập hóa đơn
   * @param maKH mã khách hàng, có thể {@code null} nếu chưa chọn khách
   * @param tenKhachHang tên khách hàng, có thể {@code null}
   * @param sdtKhachHang số điện thoại khách hàng, có thể {@code null}
   * @param luuLuc thời điểm lưu hóa đơn tạm
   * @param dongHang danh sách dòng hàng đã chọn
   */
  record HoaDonTam(
      String maTam, String maNV, String tenNhanVien,
      String maKH, String tenKhachHang, String sdtKhachHang,
      LocalDateTime luuLuc, List<DongTam> dongHang) {}

  /**
   * Lưu hóa đơn tạm; nếu {@code maTam} trống thì cấp mã mới.
   *
   * @param banTam hóa đơn tạm cần lưu
   * @return hóa đơn tạm đã lưu cùng mã và thời điểm lưu thực tế
   */
  HoaDonTam luu(HoaDonTam banTam);

  /**
   * Liệt kê hóa đơn tạm của một nhân viên, mới nhất trước.
   *
   * @param maNV mã nhân viên
   * @return danh sách hóa đơn tạm của nhân viên; rỗng nếu không có
   */
  List<HoaDonTam> findByNhanVien(String maNV);

  /**
   * Tìm hóa đơn tạm theo mã.
   *
   * @param maTam mã hóa đơn tạm
   * @return hóa đơn tạm tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<HoaDonTam> findById(String maTam);

  /**
   * Xóa hóa đơn tạm theo mã; không báo lỗi nếu mã không tồn tại.
   *
   * @param maTam mã hóa đơn tạm
   */
  void xoa(String maTam);
}
