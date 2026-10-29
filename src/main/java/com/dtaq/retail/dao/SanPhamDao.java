package com.dtaq.retail.dao;

import com.dtaq.retail.entity.SanPham;
import com.dtaq.retail.entity.TrangThaiSanPham;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Truy vấn và ghi sản phẩm theo số serial. */
public interface SanPhamDao {

  /**
   * Ghi một máy mới vào bảng {@code "SanPham"}.
   *
   * <p>Bên gọi sở hữu kết nối và giao dịch: kết nối được truyền vào không bị đóng ở đây và chế độ
   * tự động xác nhận (auto-commit) của nó không bị thay đổi. Điều này cho phép insert máy, cập nhật
   * số lượng tồn và các bước liên quan nằm cùng một giao dịch bên gọi.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param sanPham máy cần ghi; số serial phải có sẵn và không được để trống
   * @throws IllegalArgumentException nếu sản phẩm {@code null} hoặc số serial rỗng
   */
  void insert(Connection ketNoi, SanPham sanPham);

  /**
   * Tìm sản phẩm theo số serial.
   *
   * @param soSerial số serial cần tra cứu
   * @return sản phẩm tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<SanPham> findById(String soSerial);

  /**
   * Liệt kê các sản phẩm thuộc một dòng sản phẩm.
   *
   * @param maDongSP mã dòng sản phẩm
   * @return danh sách sản phẩm thuộc dòng, có thể rỗng
   */
  List<SanPham> findByDongSanPham(String maDongSP);

  /**
   * Liệt kê các sản phẩm theo trạng thái hiện tại.
   *
   * @param trangThai trạng thái cần lọc
   * @return danh sách sản phẩm có trạng thái đó, có thể rỗng
   */
  List<SanPham> findByTrangThai(TrangThaiSanPham trangThai);

  /**
   * Cập nhật trạng thái của một máy cụ thể.
   *
   * @param soSerial số serial máy cần cập nhật
   * @param trangThai trạng thái mới
   */
  void updateTrangThai(String soSerial, TrangThaiSanPham trangThai);
}
