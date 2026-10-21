package com.dtaq.retail.dao;

import java.time.Instant;
import java.util.Optional;

/** Lưu và quản lý phiếu đặt lại mật khẩu theo từng nhân viên. */
public interface PasswordResetDao {

  /**
   * Phiếu đặt lại mật khẩu của một nhân viên.
   *
   * @param maNV mã nhân viên
   * @param codeHash mã đặt lại đã băm
   * @param createdAt thời điểm tạo phiếu
   * @param expiresAt thời điểm hết hạn
   * @param soLanThu số lần nhập sai đã ghi nhận
   * @param lastSentAt thời điểm gửi mã gần nhất
   */
  record Ticket(
      String maNV,
      String codeHash,
      Instant createdAt,
      Instant expiresAt,
      int soLanThu,
      Instant lastSentAt) {}

  /**
   * Ghi phiếu mới hoặc thay thế phiếu hiện có của nhân viên.
   *
   * @param ticket phiếu cần lưu
   */
  void saveTicket(Ticket ticket);

  /**
   * Tìm phiếu đặt lại mật khẩu hiện có của nhân viên.
   *
   * @param maNV mã nhân viên
   * @return phiếu tương ứng, hoặc rỗng nếu không tồn tại
   */
  Optional<Ticket> findTicket(String maNV);

  /**
   * Cập nhật số lần nhập sai của phiếu.
   *
   * @param maNV mã nhân viên
   * @param soLanThu số lần nhập sai mới
   */
  void updateAttempts(String maNV, int soLanThu);

  /**
   * Xóa phiếu đặt lại mật khẩu của nhân viên.
   *
   * @param maNV mã nhân viên
   */
  void deleteTicket(String maNV);

  /**
   * Đặt mật khẩu mới và xóa phiếu trong cùng một giao dịch: cập nhật
   * {@code "NhanVien"."matKhau"} và xóa phiếu trong một transaction.
   *
   * @param maNV mã nhân viên
   * @param newPasswordHash mật khẩu mới đã băm (BCrypt)
   */
  void consume(String maNV, String newPasswordHash);
}
