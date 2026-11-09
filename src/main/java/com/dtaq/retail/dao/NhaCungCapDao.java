package com.dtaq.retail.dao;

import com.dtaq.retail.entity.NhaCungCap;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Truy vấn và cập nhật nhà cung cấp hàng hóa. */
public interface NhaCungCapDao {

  /**
   * Ghi một nhà cung cấp mới vào bảng {@code "NhaCungCap"}.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param nhaCungCap nhà cung cấp cần ghi; mã nhà cung cấp phải có sẵn
   * @throws IllegalArgumentException nếu nhà cung cấp {@code null} hoặc mã rỗng
   */
  void insert(Connection ketNoi, NhaCungCap nhaCungCap);

  /**
   * Tìm nhà cung cấp theo mã.
   *
   * @param maNCC mã nhà cung cấp
   * @return nhà cung cấp tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<NhaCungCap> findById(String maNCC);

  /**
   * Liệt kê tất cả nhà cung cấp.
   *
   * @return danh sách nhà cung cấp, có thể rỗng
   */
  List<NhaCungCap> findAll();

  /**
   * Cập nhật tên nhà cung cấp.
   *
   * @param maNCC mã nhà cung cấp
   * @param tenNCC tên nhà cung cấp mới
   */
  void updateTenNCC(String maNCC, String tenNCC);
}
