package com.dtaq.retail.dao;

import com.dtaq.retail.entity.ChiTietHoaDon;
import java.sql.Connection;
import java.util.List;

/** Truy cập dữ liệu dòng chi tiết hóa đơn trên bảng {@code "ChiTietHoaDon"}. */
public interface ChiTietHoaDonDao {

  /**
   * Ghi một dòng chi tiết hóa đơn.
   *
   * <p>Bên gọi sở hữu {@code ketNoi} và giao dịch tương ứng: phương thức này không đóng kết nối,
   * không tự bật hay tắt chế độ tự động xác nhận (auto-commit).
   *
   * @param ketNoi kết nối đang mở do bên gọi quản lý
   * @param chiTiet dòng chi tiết cần ghi; số lượng mỗi dòng phải bằng 1
   * @throws IllegalArgumentException khi {@code chiTiet} là {@code null}, số lượng khác 1, hoặc
   *     thiếu hóa đơn / sản phẩm / dòng sản phẩm
   */
  void insert(Connection ketNoi, ChiTietHoaDon chiTiet);

  /**
   * Lấy toàn bộ dòng chi tiết của một hóa đơn, sắp xếp theo số serial.
   *
   * @param maHD mã hóa đơn cần tra cứu
   * @return danh sách dòng chi tiết đã được nạp đầy đủ các thực thể liên quan; rỗng nếu không có
   */
  List<ChiTietHoaDon> findByHoaDon(String maHD);
}
