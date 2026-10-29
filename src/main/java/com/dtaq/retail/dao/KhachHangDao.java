package com.dtaq.retail.dao;

import com.dtaq.retail.entity.KhachHang;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Truy vấn và ghi hồ sơ khách hàng. */
public interface KhachHangDao {

  /**
   * Tìm khách hàng theo số điện thoại (so khớp chính xác).
   *
   * <p>Bảng {@code "KhachHang"} không có ràng buộc UNIQUE trên số điện thoại; việc chống trùng do
   * tầng ứng dụng đảm nhận. Nếu dữ liệu cũ lỡ có nhiều hồ sơ cùng số điện thoại, hồ sơ có mã nhỏ
   * nhất được trả về. Việc chuẩn hoá số điện thoại (bỏ khoảng trắng) do bên gọi thực hiện.
   *
   * @param soDienThoai số điện thoại đã chuẩn hoá
   * @return khách hàng tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<KhachHang> findBySdt(String soDienThoai);

  /**
   * Liệt kê toàn bộ khách hàng, sắp xếp theo mã khách hàng.
   *
   * @return danh sách khách hàng, có thể rỗng
   */
  List<KhachHang> findAll();

  /**
   * Ghi một khách hàng mới bằng kết nối riêng (tự động xác nhận).
   *
   * @param khachHang khách hàng cần ghi; mã khách hàng phải có sẵn và không được để trống
   * @throws IllegalArgumentException nếu khách hàng {@code null} hoặc mã khách hàng để trống
   */
  void insert(KhachHang khachHang);

  /**
   * Ghi một khách hàng mới trong giao dịch của bên gọi.
   *
   * <p>Kết nối được truyền vào không bị đóng ở đây và chế độ tự động xác nhận của nó không bị thay
   * đổi. Nhờ vậy bước sinh mã ({@code MaGenerator}) và bước ghi khách hàng nằm trong cùng một giao
   * dịch của bên gọi.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param khachHang khách hàng cần ghi; mã khách hàng phải có sẵn và không được để trống
   * @throws IllegalArgumentException nếu khách hàng {@code null} hoặc mã khách hàng để trống
   */
  void insert(Connection ketNoi, KhachHang khachHang);

  /**
   * Cập nhật họ tên, số điện thoại và địa chỉ của khách hàng. Mã khách hàng không đổi.
   *
   * @param khachHang khách hàng với thông tin mới; mã khách hàng phải tồn tại
   * @throws IllegalArgumentException nếu khách hàng {@code null} hoặc mã khách hàng để trống
   */
  void update(KhachHang khachHang);
}
