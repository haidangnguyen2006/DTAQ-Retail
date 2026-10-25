package com.dtaq.retail.ui;

import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.page.customer.CustomerListPage;
import com.dtaq.retail.ui.page.home.ManagerHomePage;
import com.dtaq.retail.ui.page.home.SalesHomePage;
import com.dtaq.retail.ui.page.invoice.RetailInvoicePage;
import com.dtaq.retail.ui.page.report.LongStockReportPage;
import com.dtaq.retail.ui.page.report.RevenueStatisticsPage;
import com.dtaq.retail.ui.shell.AppShellFrame;
import com.dtaq.retail.ui.shell.NavigationModel;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.shell.PlaceholderPage;
import com.dtaq.retail.util.Mailer;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;
import javax.swing.JFrame;

/** Điều phối việc chuyển đổi giữa màn hình đăng nhập, quên mật khẩu và khung giao diện chính AppShell. */
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

    AppShellFrame shell = new AppShellFrame(nhanVien, this::logout);

    if (nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY) {
      ManagerHomePage managerHome = new ManagerHomePage(nhanVien);
      RevenueStatisticsPage revenueStats = new RevenueStatisticsPage();
      LongStockReportPage longStock = new LongStockReportPage();

      revenueStats.setOnNavigate(shell::showPage);

      shell.registerPage(managerHome);
      shell.registerPage(revenueStats);
      shell.registerPage(longStock);

      registerPlaceholders(
          shell, nhanVien, Set.of(managerHome.id(), revenueStats.id(), longStock.id()));
    } else {
      SalesHomePage salesHome = new SalesHomePage(nhanVien);
      CustomerListPage customers = new CustomerListPage();
      RetailInvoicePage invoice = new RetailInvoicePage(nhanVien);

      salesHome.setOnNavigate(shell::showPage);

      shell.registerPage(salesHome);
      shell.registerPage(customers);
      shell.registerPage(invoice);

      registerPlaceholders(
          shell, nhanVien, Set.of(salesHome.id(), customers.id(), invoice.id()));
    }

    shell.showPage(NavigationModel.getDefaultPageFor(nhanVien.loaiQuyen()));
    current = shell;
    shell.setVisible(true);
  }

  /**
   * Đăng ký trang tạm cho mọi mục menu chưa có trang thật, bảo đảm không còn mục menu "chết" vì
   * {@code AppShellFrame.showPage} bỏ qua im lặng các {@code PageId} chưa được đăng ký.
   */
  private static void registerPlaceholders(
      AppShellFrame shell, NhanVien nhanVien, Set<PageId> daCoTrang) {
    String section = nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY ? "Quản lý" : "Bán hàng";
    for (NavigationModel.NavEntry entry : NavigationModel.getEntriesFor(nhanVien.loaiQuyen())) {
      if (daCoTrang.contains(entry.pageId())) {
        continue;
      }
      shell.registerPage(
          new PlaceholderPage(entry.pageId(), entry.label(), section, entry.iconName()));
    }
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
