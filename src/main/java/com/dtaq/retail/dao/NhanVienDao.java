package com.dtaq.retail.dao;

import com.dtaq.retail.entity.NhanVien;
import java.util.Optional;

/** Truy vấn và cập nhật tài khoản nhân viên. */
public interface NhanVienDao {

  /**
   * Tìm nhân viên theo tên đăng nhập.
   *
   * @param tenDangNhap tên đăng nhập cần tra cứu
   * @return nhân viên tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<NhanVien> findByTenDangNhap(String tenDangNhap);

  /**
   * Cập nhật mật khẩu đã băm của nhân viên.
   *
   * @param maNV mã nhân viên
   * @param matKhauHash mật khẩu đã băm (BCrypt), không phải mật khẩu thô
   */
  void updateMatKhau(String maNV, String matKhauHash);
}
