package com.dtaq.retail.ui;

import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.controller.BanHangController;
import com.dtaq.retail.controller.DanhMucController;
import com.dtaq.retail.controller.KhachHangController;
import com.dtaq.retail.controller.NhanVienController;
import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.controller.SanPhamController;
import com.dtaq.retail.controller.SanPhamLoiController;
import com.dtaq.retail.controller.TraCuuSanPhamController;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.dao.impl.DanhMucSanPhamDaoImpl;
import com.dtaq.retail.dao.impl.DongSanPhamImpl;
import com.dtaq.retail.dao.impl.HoaDonTamFileDaoImpl;
import com.dtaq.retail.dao.impl.KhachHangDaoImpl;
import com.dtaq.retail.dao.impl.NhaCungCapImpl;
import com.dtaq.retail.dao.impl.NhanVienDaoImpl;
import com.dtaq.retail.dao.impl.SanPhamImpl;
import com.dtaq.retail.dao.impl.SanPhamLoiDaoImpl;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.page.category.CategoriesPage;
import com.dtaq.retail.ui.page.customer.CustomerListPage;
import com.dtaq.retail.ui.page.customer.KhachHangPicker;
import com.dtaq.retail.ui.page.defective.DefectiveProductsPage;
import com.dtaq.retail.ui.page.employee.EmployeePage;
import com.dtaq.retail.ui.page.home.ManagerHomePage;
import com.dtaq.retail.ui.page.home.SalesHomePage;
import com.dtaq.retail.ui.page.invoice.RetailInvoicePage;
import com.dtaq.retail.ui.page.product.ProductLookupPage;
import com.dtaq.retail.ui.page.product.ProductPage;
import com.dtaq.retail.ui.page.report.LongStockReportPage;
import com.dtaq.retail.ui.page.report.RevenueStatisticsPage;
import com.dtaq.retail.ui.shell.AppShellFrame;
import com.dtaq.retail.ui.shell.NavigationModel;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.shell.PlaceholderPage;
import com.dtaq.retail.util.Mailer;
import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.sql.DataSource;
import javax.swing.JFrame;

/** Điều phối việc chuyển đổi giữa màn hình đăng nhập, quên mật khẩu và khung giao diện chính AppShell. */
public final class AppNavigator {

  private final NhanVienDao nhanVienDao;
  private final PasswordResetDao passwordResetDao;
  private final Mailer mailer;
  private final DataSource dataSource;
  private JFrame current;

  /**
   * Khởi tạo bộ điều phối màn hình.
   *
   * @param nhanVienDao lớp truy cập tài khoản nhân viên; không được {@code null}
   * @param passwordResetDao lớp truy cập phiếu đặt lại mật khẩu; không được {@code null}
   * @param mailer bộ gửi email xác nhận; không được {@code null}
   * @param dataSource nguồn dữ liệu dùng chung cho các màn hình nghiệp vụ; không được {@code null}
   */
  public AppNavigator(
      NhanVienDao nhanVienDao, PasswordResetDao passwordResetDao, Mailer mailer,
      DataSource dataSource) {
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
    this.passwordResetDao = Objects.requireNonNull(passwordResetDao, "passwordResetDao");
    this.mailer = Objects.requireNonNull(mailer, "mailer");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
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

    List<Page> pages = nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY
        ? managerPages(nhanVien, dataSource, shell::showPage)
        : salesPages(nhanVien, dataSource, shell::showPage);
    pages.forEach(shell::registerPage);
    registerPlaceholders(shell, nhanVien, idsOf(pages));

    shell.showPage(NavigationModel.getDefaultPageFor(nhanVien.loaiQuyen()));
    current = shell;
    shell.setVisible(true);
  }

  /**
   * Dựng danh sách trang thật dành cho vai trò quản lý. Bên gọi chịu trách nhiệm đăng ký từng trang
   * vào shell và đăng ký trang tạm cho các mục menu còn lại.
   *
   * @param nhanVien nhân viên quản lý đang đăng nhập; không được {@code null}
   * @param dataSource nguồn dữ liệu cho các lớp truy cập; không được {@code null}
   * @param onNavigate callback điều hướng giữa các trang; không được {@code null}
   * @return danh sách trang thật theo thứ tự hiển thị, mỗi {@code PageId} xuất hiện tối đa một lần
   */
  static List<Page> managerPages(
      NhanVien nhanVien, DataSource dataSource, Consumer<PageId> onNavigate) {
    Objects.requireNonNull(nhanVien, "nhanVien");
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(onNavigate, "onNavigate");

    SanPhamDao sanPhamDao = new SanPhamImpl(dataSource);
    ManagerHomePage managerHome = new ManagerHomePage(nhanVien);
    RevenueStatisticsPage revenueStats = new RevenueStatisticsPage();
    LongStockReportPage longStock = new LongStockReportPage();
    EmployeePage employees = new EmployeePage(new NhanVienController(new NhanVienDaoImpl(dataSource)));
    DefectiveProductsPage defective = new DefectiveProductsPage(
        new SanPhamLoiController(new SanPhamLoiDaoImpl(dataSource), sanPhamDao, dataSource));

    revenueStats.setOnNavigate(onNavigate);

    return List.of(managerHome, revenueStats, longStock, employees, defective);
  }

  /**
   * Dựng danh sách trang thật dành cho vai trò nhân viên bán hàng. Bên gọi chịu trách nhiệm đăng ký
   * từng trang vào shell và đăng ký trang tạm cho các mục menu còn lại.
   *
   * @param nhanVien nhân viên bán hàng đang đăng nhập; không được {@code null}
   * @param dataSource nguồn dữ liệu cho các lớp truy cập; không được {@code null}
   * @param onNavigate callback điều hướng giữa các trang; không được {@code null}
   * @return danh sách trang thật theo thứ tự hiển thị, mỗi {@code PageId} xuất hiện tối đa một lần
   */
  static List<Page> salesPages(
      NhanVien nhanVien, DataSource dataSource, Consumer<PageId> onNavigate) {
    Objects.requireNonNull(nhanVien, "nhanVien");
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(onNavigate, "onNavigate");

    SanPhamDao sanPhamDao = new SanPhamImpl(dataSource);
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(dataSource);
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(dataSource);
    KhachHangController khachHangController =
        new KhachHangController(new KhachHangDaoImpl(dataSource), dataSource);
    SanPhamLoiDao sanPhamLoiDao = new SanPhamLoiDaoImpl(dataSource);

    SalesHomePage salesHome = new SalesHomePage(nhanVien);
    CustomerListPage customers = new CustomerListPage(khachHangController);
    BanHangController banHang = new BanHangController(
        sanPhamDao,
        new HoaDonTamFileDaoImpl(
            HoaDonTamFileDaoImpl.macDinh(), Clock.systemDefaultZone()));
    RetailInvoicePage invoice =
        new RetailInvoicePage(nhanVien, banHang, new KhachHangPicker(khachHangController));
    ProductLookupPage productLookup =
        new ProductLookupPage(new TraCuuSanPhamController(sanPhamDao, dongSanPhamDao));
    ProductPage products = new ProductPage(
        new SanPhamController(sanPhamDao, dongSanPhamDao, nhaCungCapDao, dataSource), sanPhamDao);
    CategoriesPage categories =
        new CategoriesPage(new DanhMucController(new DanhMucSanPhamDaoImpl(dataSource), dataSource));
    DefectiveProductsPage defective = new DefectiveProductsPage(
        new SanPhamLoiController(sanPhamLoiDao, sanPhamDao, dataSource));

    salesHome.setOnNavigate(onNavigate);

    return List.of(salesHome, customers, invoice, productLookup, products, categories, defective);
  }

  private static Set<PageId> idsOf(List<Page> pages) {
    Set<PageId> ids = EnumSet.noneOf(PageId.class);
    for (Page page : pages) {
      ids.add(page.id());
    }
    return ids;
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
