package com.dtaq.retail.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.config.ConnectDB;
import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.shell.NavigationModel;
import com.dtaq.retail.ui.shell.Page;
import com.dtaq.retail.ui.shell.PageId;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Kiểm thử việc đăng ký trang thật theo vai trò của {@link AppNavigator}.
 *
 * <p>Mỗi vai trò chỉ được đăng ký đúng một lần cho mỗi {@link PageId}, toàn bộ {@code PageId} đăng
 * ký phải nằm trong menu của vai trò đó, và phần còn lại của menu phải do trang tạm phụ trách.
 */
class AppNavigatorPagesTest {

  private static final DataSource DS = ConnectDB.getDataSource();

  private static final Consumer<PageId> KHONG_DIEU_HUONG = id -> {
  };

  private static final NhanVien NV_QUAN_LY = new NhanVien(
      "NVQUANLY-TEST-001",
      "Nguyễn Quản Lý",
      "0900000001",
      "quanly.test@dtaq.local",
      LocalDate.of(2026, 1, 1),
      "quanly.test",
      "matkhau-test",
      LoaiQuyen.NHAN_VIEN_QUAN_LY,
      true);

  private static final NhanVien NV_BAN_HANG = new NhanVien(
      "NVBANHANG-TEST-001",
      "Trần Bán Hàng",
      "0900000002",
      "banhang.test@dtaq.local",
      LocalDate.of(2026, 1, 2),
      "banhang.test",
      "matkhau-test",
      LoaiQuyen.NHAN_VIEN_BAN_HANG,
      true);

  @BeforeAll
  static void kiemTraCsdl() {
    Assumptions.assumeTrue(ConnectDB.isAvailable(), "no database available");
  }

  @Test
  @DisplayName("Vai trò quản lý đăng ký đúng 13 trang thật theo thứ tự menu")
  void managerDangKyDung13TrangThat() {
    List<Page> pages = AppNavigator.managerPages(NV_QUAN_LY, DS, KHONG_DIEU_HUONG);

    Set<PageId> ids = idsOf(pages);
    assertEquals(Set.of(
        PageId.MANAGER_HOME,
        PageId.REVENUE_STATS,
        PageId.LONG_STOCK_REPORT,
        PageId.PRODUCT_LINES,
        PageId.DEFECTIVE_PRODUCTS,
        PageId.PROMOTIONS,
        PageId.EMPLOYEES,
        PageId.SALES_BY_PRODUCT,
        PageId.SALES_BY_EMPLOYEE,
        PageId.RETURN_APPROVALS,
        PageId.PRICE_TAX,
        PageId.BACKUP,
        PageId.RESTORE), ids);
    assertEquals(13, pages.size(), "không được đăng ký trùng PageId");
    assertEquals(List.of(
        PageId.MANAGER_HOME,
        PageId.REVENUE_STATS,
        PageId.LONG_STOCK_REPORT,
        PageId.PRODUCT_LINES,
        PageId.DEFECTIVE_PRODUCTS,
        PageId.PROMOTIONS,
        PageId.EMPLOYEES,
        PageId.SALES_BY_PRODUCT,
        PageId.SALES_BY_EMPLOYEE,
        PageId.RETURN_APPROVALS,
        PageId.PRICE_TAX,
        PageId.BACKUP,
        PageId.RESTORE), idsTheoThuTu(pages), "đúng thứ tự menu của vai trò quản lý");
  }

  @Test
  @DisplayName("Vai trò bán hàng đăng ký đúng 10 trang thật theo thứ tự menu")
  void salesDangKyDung10TrangThat() {
    List<Page> pages = AppNavigator.salesPages(NV_BAN_HANG, DS, KHONG_DIEU_HUONG);

    Set<PageId> ids = idsOf(pages);
    assertEquals(Set.of(
        PageId.SALES_HOME,
        PageId.CUSTOMERS,
        PageId.RETAIL_INVOICE,
        PageId.PRODUCT_LOOKUP,
        PageId.CUSTOMER_LOOKUP,
        PageId.RETURNS,
        PageId.PRODUCTS,
        PageId.PRODUCT_LINES,
        PageId.CATEGORIES,
        PageId.DEFECTIVE_PRODUCTS), ids);
    assertEquals(10, pages.size(), "không được đăng ký trùng PageId");
    assertEquals(List.of(
        PageId.SALES_HOME,
        PageId.CUSTOMERS,
        PageId.RETAIL_INVOICE,
        PageId.PRODUCT_LOOKUP,
        PageId.CUSTOMER_LOOKUP,
        PageId.RETURNS,
        PageId.PRODUCTS,
        PageId.PRODUCT_LINES,
        PageId.CATEGORIES,
        PageId.DEFECTIVE_PRODUCTS), idsTheoThuTu(pages),
        "đúng thứ tự menu của vai trò bán hàng");
  }

  @Test
  @DisplayName("Trang thật phủ đúng menu và phần còn lại là placeholder")
  void trangThatPhuDungMenuConLaiLaPlaceholder() {
    kiemTraPhuMenu(
        LoaiQuyen.NHAN_VIEN_QUAN_LY,
        AppNavigator.managerPages(NV_QUAN_LY, DS, KHONG_DIEU_HUONG),
        0,
        Set.of());

    kiemTraPhuMenu(
        LoaiQuyen.NHAN_VIEN_BAN_HANG,
        AppNavigator.salesPages(NV_BAN_HANG, DS, KHONG_DIEU_HUONG),
        0,
        Set.of());
  }

  private static void kiemTraPhuMenu(
      LoaiQuyen loaiQuyen, List<Page> pages, int soPlaceholder, Set<PageId> placeholderMongDoi) {
    Set<PageId> menuIds = NavigationModel.getEntriesFor(loaiQuyen).stream()
        .map(NavigationModel.NavEntry::pageId)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(PageId.class)));
    Set<PageId> ids = idsOf(pages);

    assertTrue(menuIds.containsAll(ids),
        "mọi trang thật phải có mục menu tương ứng: " + loaiQuyen);

    Set<PageId> conLai = EnumSet.copyOf(menuIds);
    conLai.removeAll(ids);
    assertEquals(soPlaceholder, conLai.size(), "số trang tạm còn lại: " + loaiQuyen);
    assertEquals(placeholderMongDoi, conLai, "đúng các trang tạm còn lại: " + loaiQuyen);
  }

  private static Set<PageId> idsOf(List<Page> pages) {
    return pages.stream()
        .map(Page::id)
        .collect(Collectors.toCollection(() -> EnumSet.noneOf(PageId.class)));
  }

  private static List<PageId> idsTheoThuTu(List<Page> pages) {
    return pages.stream().map(Page::id).toList();
  }
}
