package com.dtaq.retail.util;

/** Gửi mã xác nhận đặt lại mật khẩu tới hộp thư của nhân viên. */
public interface Mailer {

  /** Gửi mã xác nhận đặt lại mật khẩu tới địa chỉ email đã đăng ký. */
  void sendPasswordResetCode(String toEmail, String code);
}
