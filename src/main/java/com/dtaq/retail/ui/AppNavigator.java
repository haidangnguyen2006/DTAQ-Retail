package com.dtaq.retail.ui;

import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.util.Mailer;
import java.time.Clock;
import java.util.Objects;
import javax.swing.JFrame;

/** Điều phối việc chuyển đổi giữa màn hình đăng nhập, quên mật khẩu và màn hình chính. */
public final class AppNavigator {

  private final NhanVienDao nhanVienDao;
  private final PasswordResetDao passwordResetDao;
  private final Mailer mailer;
  private JFrame current;

  /**
   * Khởi tạo bộ điều phối màn hình.
   *
   * @param nhanVienDao lớp truy cập tài khoản nhân viên; không được {@code null}
   * @param passwordResetDao lớp truy cập phiếu đặt lại mật khẩu; không được {@code null}
   * @param mailer bộ gửi email xác nhận; không được {@code null}
   */
  public AppNavigator(
      NhanVienDao nhanVienDao, PasswordResetDao passwordResetDao, Mailer mailer) {
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
    this.passwordResetDao = Objects.requireNonNull(passwordResetDao, "passwordResetDao");
    this.mailer = Objects.requireNonNull(mailer, "mailer");
  }

  /** Hiển thị màn hình đăng nhập; phải gọi trên EDT. */
  public void showLogin() {
    disposeCurrent();
    LoginFrame loginFrame = new LoginFrame(new AuthController(nhanVienDao), this);
    current = loginFrame;
    loginFrame.setVisible(true);
  }

  /** Hiển thị màn hình quên mật khẩu; phải gọi trên EDT. */
  public void showForgotPassword() {
    disposeCurrent();
    ForgotPasswordFrame forgotPasswordFrame = new ForgotPasswordFrame(
        new PasswordResetController(
            nhanVienDao, passwordResetDao, mailer, Clock.systemDefaultZone()),
        this);
    current = forgotPasswordFrame;
    forgotPasswordFrame.setVisible(true);
  }

  /** Hiển thị màn hình chính cho nhân viên đã đăng nhập; phải gọi trên EDT. */
  public void showMain(NhanVien nhanVien) {
    Objects.requireNonNull(nhanVien, "nhanVien");
    CurrentSession.set(nhanVien);
    disposeCurrent();
    MainFrame mainFrame = new MainFrame(nhanVien, this::logout);
    current = mainFrame;
    mainFrame.setVisible(true);
  }

  /** Xóa phiên hiện tại và quay lại màn hình đăng nhập; phải gọi trên EDT. */
  public void logout() {
    CurrentSession.clear();
    showLogin();
  }

  private void disposeCurrent() {
    if (current != null) {
      current.dispose();
      current = null;
    }
  }
}
