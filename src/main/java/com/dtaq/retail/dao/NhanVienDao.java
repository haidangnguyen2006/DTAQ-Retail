package com.dtaq.retail.dao;

import com.dtaq.retail.entity.NhanVien;
import java.sql.Connection;
import java.util.List;
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

  /**
   * Liệt kê toàn bộ nhân viên (kể cả tài khoản đã khóa), sắp xếp theo mã nhân viên.
   *
   * @return danh sách nhân viên, có thể rỗng
   */
  List<NhanVien> findAll();

  /**
   * Tìm nhân viên theo mã nhân viên.
   *
   * @param maNV mã nhân viên cần tra cứu
   * @return nhân viên tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<NhanVien> findByMa(String maNV);

  /**
   * Ghi một nhân viên mới trong giao dịch của bên gọi.
   *
   * <p>Kết nối được truyền vào không bị đóng ở đây và chế độ tự động xác nhận của nó không bị thay
   * đổi. Nhờ vậy bước sinh mã ({@code MaGenerator}) và bước ghi nhân viên nằm trong cùng một giao
   * dịch của bên gọi.
   *
   * @param ketNoi kết nối của bên gọi; không được {@code null}
   * @param nhanVien nhân viên cần ghi; mã nhân viên phải có sẵn, mật khẩu phải là chuỗi đã băm
   *     (BCrypt), không phải mật khẩu thô
   * @throws IllegalArgumentException nếu nhân viên {@code null} hoặc mã nhân viên để trống
   */
  void insert(Connection ketNoi, NhanVien nhanVien);

  /**
   * Cập nhật thông tin hồ sơ của nhân viên: họ tên, số điện thoại, email và ngày vào làm.
   *
   * <p>Mã nhân viên, tên đăng nhập, mật khẩu, loại quyền và trạng thái hoạt động không bị thay đổi
   * bởi phương thức này; mật khẩu dùng {@link #updateMatKhau}, trạng thái dùng
   * {@link #updateTrangThai}.
   *
   * @param nhanVien nhân viên với thông tin mới; mã nhân viên phải tồn tại
   * @throws IllegalArgumentException nếu nhân viên {@code null} hoặc mã nhân viên để trống
   */
  void update(NhanVien nhanVien);

  /**
   * Đổi trạng thái hoạt động của tài khoản (khóa khi nghỉ việc, mở lại khi cần). Nhân viên không bao
   * giờ bị xóa để giữ lịch sử giao dịch.
   *
   * @param maNV mã nhân viên
   * @param trangThaiHoatDong {@code true} để mở, {@code false} để khóa tài khoản
   */
  void updateTrangThai(String maNV, boolean trangThaiHoatDong);

  /**
   * Kiểm tra tên đăng nhập đã được dùng hay chưa.
   *
   * @param tenDangNhap tên đăng nhập cần kiểm tra
   * @return {@code true} nếu đã có nhân viên dùng tên đăng nhập này
   */
  boolean existsTenDangNhap(String tenDangNhap);

  /**
   * Kiểm tra mã nhân viên đã tồn tại hay chưa.
   *
   * @param maNV mã nhân viên cần kiểm tra
   * @return {@code true} nếu mã nhân viên đã tồn tại
   */
  boolean existsMaNV(String maNV);
}
