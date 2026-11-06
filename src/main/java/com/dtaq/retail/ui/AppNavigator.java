package com.dtaq.retail.ui;

import com.dtaq.retail.config.ReturnConfig;
import com.dtaq.retail.controller.AuthController;
import com.dtaq.retail.controller.BackupController;
import com.dtaq.retail.controller.BanHangController;
import com.dtaq.retail.controller.BaoCaoTonKhoController;
import com.dtaq.retail.controller.DanhMucController;
import com.dtaq.retail.controller.DoiTraController;
import com.dtaq.retail.controller.DongSanPhamController;
import com.dtaq.retail.controller.GiaThueController;
import com.dtaq.retail.controller.IncidentController;
import com.dtaq.retail.controller.KhachHangController;
import com.dtaq.retail.controller.KhuyenMaiController;
import com.dtaq.retail.controller.NhanVienController;
import com.dtaq.retail.controller.ThongKeDoanhThuController;
import com.dtaq.retail.controller.ThongKeNhanVienController;
import com.dtaq.retail.controller.ThongKeSanPhamController;
import com.dtaq.retail.controller.PasswordResetController;
import com.dtaq.retail.controller.PheDuyetDoiTraController;
import com.dtaq.retail.controller.SanPhamController;
import com.dtaq.retail.controller.SanPhamLoiController;
import com.dtaq.retail.controller.TraCuuKhachHangController;
import com.dtaq.retail.controller.TraCuuSanPhamController;
import com.dtaq.retail.dao.DanhMucSanPhamDao;
import com.dtaq.retail.dao.DongSanPhamDao;
import com.dtaq.retail.dao.NhaCungCapDao;
import com.dtaq.retail.dao.NhanVienDao;
import com.dtaq.retail.dao.PasswordResetDao;
import com.dtaq.retail.dao.PhieuDuyet;
import com.dtaq.retail.dao.SanPhamDao;
import com.dtaq.retail.dao.SanPhamLoiDao;
import com.dtaq.retail.dao.impl.BaoCaoTonKhoDaoImpl;
import com.dtaq.retail.dao.impl.ChiTietHoaDonDaoImpl;
import com.dtaq.retail.dao.impl.DanhMucSanPhamDaoImpl;
import com.dtaq.retail.dao.impl.DongSanPhamImpl;
import com.dtaq.retail.dao.impl.HoaDonDaoImpl;
import com.dtaq.retail.dao.impl.HoaDonTamFileDaoImpl;
import com.dtaq.retail.dao.impl.KhachHangDaoImpl;
import com.dtaq.retail.dao.impl.KhachHangGiaoDichDaoImpl;
import com.dtaq.retail.dao.impl.KhuyenMaiDaoImpl;
import com.dtaq.retail.dao.impl.NhaCungCapImpl;
import com.dtaq.retail.dao.impl.NhanVienDaoImpl;
import com.dtaq.retail.dao.impl.PhieuDoiDaoImpl;
import com.dtaq.retail.dao.impl.PhieuTraDaoImpl;
import com.dtaq.retail.dao.impl.SanPhamImpl;
import com.dtaq.retail.dao.impl.SanPhamLoiDaoImpl;
import com.dtaq.retail.dao.impl.ThongKeDoanhThuDaoImpl;
import com.dtaq.retail.dao.impl.ThongKeNhanVienDaoImpl;
import com.dtaq.retail.dao.impl.ThongKeSanPhamDaoImpl;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.service.BackupScheduler;
import com.dtaq.retail.service.BackupService;
import com.dtaq.retail.service.RestoreService;
import com.dtaq.retail.session.CurrentSession;
import com.dtaq.retail.ui.page.approval.ReturnApprovalsPage;
import com.dtaq.retail.ui.page.backup.BackupPage;
import com.dtaq.retail.ui.page.backup.IncidentDialog;
import com.dtaq.retail.ui.page.backup.RestorePage;
import com.dtaq.retail.ui.page.category.CategoriesPage;
import com.dtaq.retail.ui.page.customer.CustomerListPage;
import com.dtaq.retail.ui.page.customer.CustomerLookupPage;
import com.dtaq.retail.ui.page.customer.KhachHangPicker;
import com.dtaq.retail.ui.page.defective.DefectiveProductsPage;
import com.dtaq.retail.ui.page.employee.EmployeePage;
import com.dtaq.retail.ui.page.home.ManagerHomePage;
import com.dtaq.retail.ui.page.home.ManagerHomePage.ApprovalRow;
import com.dtaq.retail.ui.page.home.SalesHomePage;
import com.dtaq.retail.ui.page.invoice.RetailInvoicePage;
import com.dtaq.retail.ui.page.product.PriceTaxPage;
import com.dtaq.retail.ui.page.product.ProductLinesPage;
import com.dtaq.retail.ui.page.product.ProductLookupPage;
import com.dtaq.retail.ui.page.product.ProductPage;
import com.dtaq.retail.ui.page.promotion.PromotionsPage;
import com.dtaq.retail.ui.page.report.LongStockReportPage;
import com.dtaq.retail.ui.page.report.RevenueStatisticsPage;
import com.dtaq.retail.ui.page.report.SalesByEmployeePage;
import com.dtaq.retail.ui.page.report.SalesByProductPage;
import com.dtaq.retail.ui.page.returns.ReturnsPage;
import com.dtaq.retail.ui.shell.AppShellFrame;
import com.dtaq.retail.ui.shell.NavigationModel;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import com.dtaq.retail.ui.shell.PlaceholderPage;
import com.dtaq.retail.util.Mailer;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import javax.sql.DataSource;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Điều phối việc chuyển đổi giữa màn hình đăng nhập, quên mật khẩu và khung giao diện chính AppShell. */
public final class AppNavigator {

  private static final Logger log = LoggerFactory.getLogger(AppNavigator.class);

  private final NhanVienDao nhanVienDao;
  private final PasswordResetDao passwordResetDao;
  private final Mailer mailer;
  private final DataSource dataSource;
  private final BackupScheduler backupScheduler;
  private JFrame current;

  /**
   * Khởi tạo bộ điều phối màn hình không gắn với bộ lập lịch sao lưu.
   *
   * @param nhanVienDao lớp truy cập tài khoản nhân viên; không được {@code null}
   * @param passwordResetDao lớp truy cập phiếu đặt lại mật khẩu; không được {@code null}
   * @param mailer bộ gửi email xác nhận; không được {@code null}
   * @param dataSource nguồn dữ liệu dùng chung cho các màn hình nghiệp vụ; không được {@code null}
   */
  public AppNavigator(
      NhanVienDao nhanVienDao, PasswordResetDao passwordResetDao, Mailer mailer,
      DataSource dataSource) {
    this(nhanVienDao, passwordResetDao, mailer, dataSource, null);
  }

  /**
   * Khởi tạo bộ điều phối màn hình, có thể gắn với bộ lập lịch sao lưu tự động.
   *
   * @param nhanVienDao lớp truy cập tài khoản nhân viên; không được {@code null}
   * @param passwordResetDao lớp truy cập phiếu đặt lại mật khẩu; không được {@code null}
   * @param mailer bộ gửi email xác nhận; không được {@code null}
   * @param dataSource nguồn dữ liệu dùng chung cho các màn hình nghiệp vụ; không được {@code null}
   * @param backupScheduler bộ lập lịch cần lập lại khi Quản lý lưu cấu hình; có thể {@code null}
   */
  public AppNavigator(
      NhanVienDao nhanVienDao, PasswordResetDao passwordResetDao, Mailer mailer,
      DataSource dataSource, BackupScheduler backupScheduler) {
    this.nhanVienDao = Objects.requireNonNull(nhanVienDao, "nhanVienDao");
    this.passwordResetDao = Objects.requireNonNull(passwordResetDao, "passwordResetDao");
    this.mailer = Objects.requireNonNull(mailer, "mailer");
    this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    this.backupScheduler = backupScheduler;
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
        ? managerPages(nhanVien, dataSource, shell::showPage, backupScheduler)
        : salesPages(nhanVien, dataSource, shell::showPage);
    pages.forEach(shell::registerPage);
    registerPlaceholders(shell, nhanVien, idsOf(pages));

    shell.showPage(NavigationModel.getDefaultPageFor(nhanVien.loaiQuyen()));
    current = shell;
    shell.setVisible(true);

    if (nhanVien.loaiQuyen() == LoaiQuyen.NHAN_VIEN_QUAN_LY) {
      SwingUtilities.invokeLater(() -> {
        try {
          IncidentController incidentController = IncidentController.macDinh(dataSource);
          incidentController.suCoDangCho().ifPresent(i ->
              new IncidentDialog(shell, incidentController, i, shell::showPage).setVisible(true));
        } catch (RuntimeException e) {
          log.warn("Không hiển thị được sự cố dữ liệu: {}", e.getMessage());
        }
      });
    }
  }

  /**
   * Dựng danh sách trang thật dành cho vai trò quản lý. Bên gọi chịu trách nhiệm đăng ký từng trang
   * vào shell và đăng ký trang tạm cho các mục menu còn lại.
   *
   * @param nhanVien nhân viên quản lý đang đăng nhập; không được {@code null}
   * @param dataSource nguồn dữ liệu cho các lớp truy cập; không được {@code null}
   * @param onNavigate callback điều hướng giữa các trang; không được {@code null}
   * @return 13 trang thật theo đúng thứ tự menu: {@link PageId#MANAGER_HOME}, {@link
   *     PageId#REVENUE_STATS}, {@link PageId#LONG_STOCK_REPORT}, {@link PageId#PRODUCT_LINES},
   *     {@link PageId#DEFECTIVE_PRODUCTS}, {@link PageId#PROMOTIONS}, {@link PageId#EMPLOYEES}, {@link PageId#SALES_BY_PRODUCT}, {@link
   *     PageId#SALES_BY_EMPLOYEE},
   *     {@link PageId#RETURN_APPROVALS}, {@link PageId#PRICE_TAX}, {@link PageId#BACKUP}, {@link
   *     PageId#RESTORE}; mỗi {@code PageId} xuất hiện tối đa một lần
   */
  static List<Page> managerPages(
      NhanVien nhanVien, DataSource dataSource, Consumer<PageId> onNavigate) {
    return managerPages(nhanVien, dataSource, onNavigate, null);
  }

  /**
   * Dựng danh sách trang thật dành cho vai trò quản lý, có gắn bộ lập lịch sao lưu tự động. Bên gọi
   * chịu trách nhiệm đăng ký từng trang vào shell và đăng ký trang tạm cho các mục menu còn lại.
   *
   * @param nhanVien nhân viên quản lý đang đăng nhập; không được {@code null}
   * @param dataSource nguồn dữ liệu cho các lớp truy cập; không được {@code null}
   * @param onNavigate callback điều hướng giữa các trang; không được {@code null}
   * @param backupScheduler bộ lập lịch cần lập lại khi lưu cấu hình trên trang Sao lưu; có thể
   *     {@code null}
   * @return 13 trang thật theo đúng thứ tự menu: {@link PageId#MANAGER_HOME}, {@link
   *     PageId#REVENUE_STATS}, {@link PageId#LONG_STOCK_REPORT}, {@link PageId#PRODUCT_LINES},
   *     {@link PageId#DEFECTIVE_PRODUCTS}, {@link PageId#PROMOTIONS}, {@link PageId#EMPLOYEES}, {@link PageId#SALES_BY_PRODUCT}, {@link
   *     PageId#SALES_BY_EMPLOYEE},
   *     {@link PageId#RETURN_APPROVALS}, {@link PageId#PRICE_TAX}, {@link PageId#BACKUP}, {@link
   *     PageId#RESTORE}; mỗi {@code PageId} xuất hiện tối đa một lần
   */
  static List<Page> managerPages(
      NhanVien nhanVien, DataSource dataSource, Consumer<PageId> onNavigate,
      BackupScheduler backupScheduler) {
    Objects.requireNonNull(nhanVien, "nhanVien");
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(onNavigate, "onNavigate");

    SanPhamDao sanPhamDao = new SanPhamImpl(dataSource);
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(dataSource);
    DanhMucSanPhamDao danhMucDao = new DanhMucSanPhamDaoImpl(dataSource);
    PheDuyetDoiTraController pheDuyet = new PheDuyetDoiTraController(
        new PhieuTraDaoImpl(dataSource), new PhieuDoiDaoImpl(dataSource),
        Clock.systemDefaultZone());

    ManagerHomePage managerHome = new ManagerHomePage(nhanVien);
    RevenueStatisticsPage revenueStats = new RevenueStatisticsPage(
        new ThongKeDoanhThuController(
            new ThongKeDoanhThuDaoImpl(dataSource), new HoaDonDaoImpl(dataSource)));
    LongStockReportPage longStock = new LongStockReportPage(
        new BaoCaoTonKhoController(new BaoCaoTonKhoDaoImpl(dataSource)));
    ProductLinesPage productLines = new ProductLinesPage(
        new DongSanPhamController(dongSanPhamDao, danhMucDao, dataSource), danhMucDao);
    DefectiveProductsPage defective = new DefectiveProductsPage(
        new SanPhamLoiController(new SanPhamLoiDaoImpl(dataSource), sanPhamDao, dataSource));
    PromotionsPage promotions = new PromotionsPage(
        new KhuyenMaiController(new KhuyenMaiDaoImpl(dataSource), dongSanPhamDao, dataSource));
    EmployeePage employees = new EmployeePage(
        new NhanVienController(new NhanVienDaoImpl(dataSource), dataSource));
    SalesByProductPage salesByProduct = new SalesByProductPage(
        new ThongKeSanPhamController(new ThongKeSanPhamDaoImpl(dataSource)));
    SalesByEmployeePage salesByEmployee = new SalesByEmployeePage(new ThongKeNhanVienController(
        new ThongKeNhanVienDaoImpl(dataSource), new NhanVienDaoImpl(dataSource)));
    ReturnApprovalsPage returnApprovals = new ReturnApprovalsPage(pheDuyet);
    PriceTaxPage priceTax = new PriceTaxPage(
        new GiaThueController(dongSanPhamDao), dongSanPhamDao);
    BackupService backupService = BackupService.macDinh(dataSource);
    BackupController backupController = new BackupController(
        backupService, RestoreService.macDinh(dataSource, backupService), backupScheduler);
    BackupPage backup = new BackupPage(backupController);
    RestorePage restore = new RestorePage(backupController);
    revenueStats.setOnNavigate(onNavigate);

    managerHome.setOnApprove(
        (maPhieu, yKien) -> quyetDinhRoiTaiLai(pheDuyet, managerHome, maPhieu, true, yKien));
    managerHome.setOnReject(
        (maPhieu, yKien) -> quyetDinhRoiTaiLai(pheDuyet, managerHome, maPhieu, false, yKien));

    List<Page> pages = List.of(
        managerHome,
        revenueStats,
        longStock,
        productLines,
        defective,
        promotions,
        employees,
        salesByProduct,
        salesByEmployee,
        returnApprovals,
        priceTax,
        backup,
        restore);
    taiDanhSachChoDuyet(pheDuyet, managerHome);
    return pages;
  }

  /**
   * Ghi quyết định phê duyệt/từ chối ở luồng nền rồi tải lại danh sách chờ duyệt của trang chủ.
   * Không để ngoại lệ thoát khỏi luồng nền.
   */
  private static void quyetDinhRoiTaiLai(
      PheDuyetDoiTraController pheDuyet,
      ManagerHomePage managerHome,
      String maPhieu,
      boolean duyet,
      String yKien) {
    new SwingWorker<Boolean, Void>() {
      @Override
      protected Boolean doInBackground() {
        return pheDuyet.quyetDinh(maPhieu, duyet, yKien).thanhCong();
      }

      @Override
      protected void done() {
        try {
          get();
          taiDanhSachChoDuyet(pheDuyet, managerHome);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          managerHome.showError("Không thể ghi quyết định phê duyệt");
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            managerHome.showError("Chỉ Quản lý được phê duyệt yêu cầu đổi trả");
          } else {
            managerHome.showError("Không thể ghi quyết định phê duyệt");
          }
        } catch (SecurityException e) {
          managerHome.showError("Chỉ Quản lý được phê duyệt yêu cầu đổi trả");
        }
      }
    }.execute();
  }

  /**
   * Tải danh sách phiếu chờ duyệt ở luồng nền và đẩy sang bảng của trang chủ Quản lý.
   * Không để ngoại lệ thoát khỏi luồng nền.
   */
  private static void taiDanhSachChoDuyet(
      PheDuyetDoiTraController pheDuyet, ManagerHomePage managerHome) {
    managerHome.showLoading();

    new SwingWorker<List<PhieuDuyet>, Void>() {
      @Override
      protected List<PhieuDuyet> doInBackground() {
        return pheDuyet.danhSachChoDuyet();
      }

      @Override
      protected void done() {
        try {
          List<ApprovalRow> rows = new ArrayList<>();
          for (PhieuDuyet phieu : get()) {
            rows.add(new ApprovalRow(
                phieu.maPhieu(),
                phieu.maHDGoc(),
                phieu.tenKhachHang(),
                phieu.soDienThoai(),
                phieu.hoanDuKien() == null ? 0L : phieu.hoanDuKien().longValueExact(),
                phieu.tenNhanVienLap(),
                phieu.lyDo() == null ? "" : phieu.lyDo()));
          }
          managerHome.renderApprovals(rows);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          managerHome.showError("Không thể tải danh sách phiếu chờ duyệt");
        } catch (ExecutionException e) {
          if (e.getCause() instanceof SecurityException) {
            managerHome.showError("Chỉ Quản lý được phê duyệt yêu cầu đổi trả");
          } else {
            managerHome.showError("Không thể tải danh sách phiếu chờ duyệt");
          }
        } catch (SecurityException e) {
          managerHome.showError("Chỉ Quản lý được phê duyệt yêu cầu đổi trả");
        }
      }
    }.execute();
  }

  /**
   * Dựng danh sách trang thật dành cho vai trò nhân viên bán hàng. Bên gọi chịu trách nhiệm đăng ký
   * từng trang vào shell và đăng ký trang tạm cho các mục menu còn lại.
   *
   * @param nhanVien nhân viên bán hàng đang đăng nhập; không được {@code null}
   * @param dataSource nguồn dữ liệu cho các lớp truy cập; không được {@code null}
   * @param onNavigate callback điều hướng giữa các trang; không được {@code null}
   * @return 10 trang thật theo đúng thứ tự menu: {@link PageId#SALES_HOME}, {@link
   *     PageId#CUSTOMERS}, {@link PageId#RETAIL_INVOICE}, {@link PageId#PRODUCT_LOOKUP}, {@link
   *     PageId#CUSTOMER_LOOKUP}, {@link PageId#RETURNS}, {@link PageId#PRODUCTS}, {@link
   *     PageId#PRODUCT_LINES}, {@link PageId#CATEGORIES}, {@link PageId#DEFECTIVE_PRODUCTS}; mỗi
   *     {@code PageId} xuất hiện tối đa một lần
   */
  static List<Page> salesPages(
      NhanVien nhanVien, DataSource dataSource, Consumer<PageId> onNavigate) {
    Objects.requireNonNull(nhanVien, "nhanVien");
    Objects.requireNonNull(dataSource, "dataSource");
    Objects.requireNonNull(onNavigate, "onNavigate");

    SanPhamDao sanPhamDao = new SanPhamImpl(dataSource);
    DongSanPhamDao dongSanPhamDao = new DongSanPhamImpl(dataSource);
    DanhMucSanPhamDao danhMucDao = new DanhMucSanPhamDaoImpl(dataSource);
    NhaCungCapDao nhaCungCapDao = new NhaCungCapImpl(dataSource);
    KhachHangController khachHangController =
        new KhachHangController(new KhachHangDaoImpl(dataSource), dataSource);
    SanPhamLoiDao sanPhamLoiDao = new SanPhamLoiDaoImpl(dataSource);

    SalesHomePage salesHome = new SalesHomePage(nhanVien);
    CustomerListPage customers = new CustomerListPage(khachHangController);
    BanHangController banHang = new BanHangController(
        sanPhamDao,
        new KhuyenMaiDaoImpl(dataSource),
        new HoaDonDaoImpl(dataSource),
        new ChiTietHoaDonDaoImpl(dataSource),
        new HoaDonTamFileDaoImpl(
            HoaDonTamFileDaoImpl.macDinh(), Clock.systemDefaultZone()),
        dataSource,
        Clock.systemDefaultZone());
    RetailInvoicePage invoice =
        new RetailInvoicePage(nhanVien, banHang, new KhachHangPicker(khachHangController));
    ProductLookupPage productLookup =
        new ProductLookupPage(new TraCuuSanPhamController(sanPhamDao, dongSanPhamDao));
    CustomerLookupPage customerLookup = new CustomerLookupPage(
        new TraCuuKhachHangController(
            new KhachHangDaoImpl(dataSource), new KhachHangGiaoDichDaoImpl(dataSource)));
    ProductPage products = new ProductPage(
        new SanPhamController(sanPhamDao, dongSanPhamDao, nhaCungCapDao, dataSource),
        sanPhamDao,
        nhaCungCapDao,
        dongSanPhamDao);
    ProductLinesPage productLines = new ProductLinesPage(
        new DongSanPhamController(dongSanPhamDao, danhMucDao, dataSource), danhMucDao);
    CategoriesPage categories = new CategoriesPage(
        new DanhMucController(danhMucDao, dataSource));
    DefectiveProductsPage defective = new DefectiveProductsPage(
        new SanPhamLoiController(sanPhamLoiDao, sanPhamDao, dataSource));
    DoiTraController doiTra = new DoiTraController(
        new PhieuTraDaoImpl(dataSource),
        new PhieuDoiDaoImpl(dataSource),
        sanPhamLoiDao,
        new HoaDonDaoImpl(dataSource),
        new ChiTietHoaDonDaoImpl(dataSource),
        sanPhamDao,
        dongSanPhamDao,
        dataSource,
        ReturnConfig.soNgayDoiTra(),
        Clock.systemDefaultZone());
    ReturnsPage returns = new ReturnsPage(doiTra);

    salesHome.setOnNavigate(onNavigate);

    return List.of(
        salesHome,
        customers,
        invoice,
        productLookup,
        customerLookup,
        returns,
        products,
        productLines,
        categories,
        defective);
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
