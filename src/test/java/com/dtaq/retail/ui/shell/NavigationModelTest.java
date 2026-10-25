package com.dtaq.retail.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dtaq.retail.entity.LoaiQuyen;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Kiểm tra mô hình điều hướng NavigationModel")
class NavigationModelTest {

  private static List<NavigationModel.NavEntry> salesEntries() {
    return NavigationModel.getEntriesFor(LoaiQuyen.NHAN_VIEN_BAN_HANG);
  }

  private static List<NavigationModel.NavEntry> managerEntries() {
    return NavigationModel.getEntriesFor(LoaiQuyen.NHAN_VIEN_QUAN_LY);
  }

  @Test
  @DisplayName("Nhân viên bán hàng có đúng 10 mục menu và không có mục Thống kê")
  void testSalesNavEntries() {
    List<NavigationModel.NavEntry> entries = salesEntries();
    assertEquals(10, entries.size());

    assertEquals(
        List.of(
            PageId.SALES_HOME,
            PageId.CUSTOMERS,
            PageId.RETAIL_INVOICE,
            PageId.PRODUCT_LOOKUP,
            PageId.CUSTOMER_LOOKUP,
            PageId.RETURNS,
            PageId.PRODUCTS,
            PageId.PRODUCT_LINES,
            PageId.CATEGORIES,
            PageId.DEFECTIVE_PRODUCTS
        ),
        entries.stream().map(NavigationModel.NavEntry::pageId).toList()
    );

    for (NavigationModel.NavEntry entry : entries) {
      assertFalse(entry.label().toLowerCase().contains("thống kê"), "Menu NVBH không được chứa mục Thống kê");
    }

    assertEquals("Tra cứu sản phẩm và tồn kho", byId(entries, "product_lookup").label());
  }

  @Test
  @DisplayName("Nhân viên quản lý có đúng 13 mục menu theo Figma")
  void testManagerNavEntries() {
    List<NavigationModel.NavEntry> entries = managerEntries();
    assertEquals(13, entries.size());

    assertEquals(
        List.of(
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
            PageId.RESTORE
        ),
        entries.stream().map(NavigationModel.NavEntry::pageId).toList()
    );

    assertEquals("Quản lý khuyến mãi", byId(entries, "promotions").label());
    assertEquals("Báo cáo tồn kho lâu ngày", byId(entries, "long_stock").label());
  }

  @Test
  @DisplayName("Mỗi vai trò có id, label và pageId duy nhất, không rỗng")
  void testEntriesHaveUniqueIdsLabelsAndPageIds() {
    for (List<NavigationModel.NavEntry> entries : List.of(salesEntries(), managerEntries())) {
      Set<String> ids = new HashSet<>();
      Set<String> labels = new HashSet<>();
      Set<PageId> pageIds = new HashSet<>();

      for (NavigationModel.NavEntry entry : entries) {
        assertFalse(entry.id().isBlank(), "id không được rỗng");
        assertFalse(entry.label().isBlank(), "label không được rỗng: " + entry.id());
        assertFalse(entry.iconName().isBlank(), "iconName không được rỗng: " + entry.id());
        assertNotNull(entry.pageId(), "pageId không được null: " + entry.id());
        ids.add(entry.id());
        labels.add(entry.label());
        pageIds.add(entry.pageId());
      }

      assertEquals(entries.size(), ids.size(), "id bị trùng");
      assertEquals(entries.size(), labels.size(), "label bị trùng");
      assertEquals(entries.size(), pageIds.size(), "pageId bị trùng");
    }
  }

  @Test
  @DisplayName("Mọi iconName đều có tệp SVG tương ứng trong resources")
  void testIconNamesResolveToExistingSvgFiles() {
    for (List<NavigationModel.NavEntry> entries : List.of(salesEntries(), managerEntries())) {
      for (NavigationModel.NavEntry entry : entries) {
        assertNotNull(
            getClass().getResource("/com/dtaq/retail/ui/theme/icons/" + entry.iconName() + ".svg"),
            "icon thiếu: " + entry.iconName()
        );
      }
    }
  }

  @Test
  @DisplayName("Trang mặc định theo vai trò không thay đổi")
  void testDefaultPages() {
    assertEquals(PageId.SALES_HOME, NavigationModel.getDefaultPageFor(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    assertEquals(PageId.MANAGER_HOME, NavigationModel.getDefaultPageFor(LoaiQuyen.NHAN_VIEN_QUAN_LY));
  }

  private static NavigationModel.NavEntry byId(List<NavigationModel.NavEntry> entries, String id) {
    return entries.stream()
        .filter(entry -> entry.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Không tìm thấy mục menu: " + id));
  }
}
