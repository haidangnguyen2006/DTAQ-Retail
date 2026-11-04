package com.dtaq.retail.dao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Yêu cầu trả hàng hoặc đổi hàng đang chờ Quản lý phê duyệt (UC018).
 *
 * <p>Đây là mô hình chỉ đọc dùng cho màn hình duyệt yêu cầu: mỗi bản ghi gộp các thông tin cần
 * thiết để Quản lý ra quyết định mà không phải tải toàn bộ phiếu trả/phiếu đổi kèm chi tiết.
 *
 * @param loai loại phiếu đang chờ duyệt ({@link LoaiPhieu#PHIEU_TRA} hoặc {@link LoaiPhieu#PHIEU_DOI})
 * @param maPhieu mã phiếu (dạng {@code PT-YYYY-NNNNNN} hoặc {@code PD-YYYY-NNNNNN})
 * @param maHDGoc mã hóa đơn gốc mà phiếu trả/phiếu đổi tham chiếu
 * @param tenKhachHang họ tên khách hàng của hóa đơn gốc
 * @param soDienThoai số điện thoại khách hàng của hóa đơn gốc
 * @param hoanDuKien số tiền dự kiến hoàn cho khách; với phiếu đổi hàng luôn bằng {@code 0}
 * @param tenNhanVienLap họ tên nhân viên đã lập phiếu
 * @param lyDo lý do trả/đổi do nhân viên ghi nhận, nhiều dòng chi tiết được nối bằng {@code "; "}
 * @param ngayTao ngày lập phiếu
 */
public record PhieuDuyet(
    LoaiPhieu loai,
    String maPhieu,
    String maHDGoc,
    String tenKhachHang,
    String soDienThoai,
    BigDecimal hoanDuKien,
    String tenNhanVienLap,
    String lyDo,
    LocalDate ngayTao) {

  /** Loại phiếu cần Quản lý phê duyệt. */
  public enum LoaiPhieu {
    /** Phiếu trả hàng, hoàn tiền cho khách. */
    PHIEU_TRA,
    /** Phiếu đổi hàng sang máy khác. */
    PHIEU_DOI
  }
}
