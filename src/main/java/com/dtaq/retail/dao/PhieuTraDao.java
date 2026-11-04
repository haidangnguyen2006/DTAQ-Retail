package com.dtaq.retail.dao;

import com.dtaq.retail.entity.TrangThaiDuyet;
import java.time.LocalDate;
import java.util.List;

/**
 * Giao diện truy cập dữ liệu phiếu trả hàng trên bảng {@code "PhieuTra"}.
 *
 * <p>Phạm vi của giao diện này chỉ gồm nghiệp vụ duyệt yêu cầu trả hàng (UC018): đọc danh sách
 * phiếu đang chờ duyệt và ghi quyết định duyệt/từ chối của Quản lý.
 */
public interface PhieuTraDao {

  /**
   * Lấy danh sách phiếu trả đang ở trạng thái {@code CHO_DUYET}.
   *
   * <p>Kết quả sắp xếp theo ngày tạo tăng dần, sau đó theo mã phiếu để thứ tự hiển thị ổn định.
   *
   * @return danh sách phiếu chờ duyệt, không bao giờ {@code null}, có thể rỗng
   */
  List<PhieuDuyet> findChoDuyet();

  /**
   * Ghi quyết định duyệt hoặc từ chối cho một phiếu trả.
   *
   * <p>Chỉ những dòng còn {@code "trangThaiDuyet" = 'CHO_DUYET'} mới được cập nhật, nhờ đó hai
   * người duyệt không thể ghi đè quyết định của nhau.
   *
   * @param maPhieu mã phiếu trả cần cập nhật
   * @param quyetDinh quyết định của Quản lý ({@link TrangThaiDuyet#DA_DUYET} hoặc
   *     {@link TrangThaiDuyet#TU_CHOI}); không được {@code null}
   * @param maNhanVienDuyet mã nhân viên Quản lý ra quyết định; không được {@code null}
   * @param ngayDuyet ngày ra quyết định; không được {@code null}
   * @param yKien ý kiến/lý do của Quản lý, được lưu nguyên văn; có thể {@code null}
   * @return {@code true} nếu cập nhật đúng một dòng, {@code false} nếu không có dòng nào bị ảnh hưởng
   */
  boolean capNhatQuyetDinh(
      String maPhieu,
      TrangThaiDuyet quyetDinh,
      String maNhanVienDuyet,
      LocalDate ngayDuyet,
      String yKien);
}
