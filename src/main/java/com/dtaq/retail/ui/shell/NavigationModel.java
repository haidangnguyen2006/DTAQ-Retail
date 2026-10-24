package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.LoaiQuyen;
import java.util.List;
import java.util.Objects;

/**
 * Mô hình điều hướng cung cấp danh mục menu và trang mặc định theo vai trò nhân viên.
 */
public final class NavigationModel {

  public record NavEntry(String id, String label, String iconName, PageId pageId) {
    public NavEntry {
      Objects.requireNonNull(id, "id");
      Objects.requireNonNull(label, "label");
      Objects.requireNonNull(iconName, "iconName");
      Objects.requireNonNull(pageId, "pageId");
    }
  }

  private NavigationModel() {
  }

  public static List<NavEntry> getEntriesFor(LoaiQuyen loaiQuyen) {
    Objects.requireNonNull(loaiQuyen, "loaiQuyen");
    if (loaiQuyen == LoaiQuyen.NHAN_VIEN_QUAN_LY) {
      return List.of(
          new NavEntry("home", "Trang chủ", "layout-dashboard", PageId.MANAGER_HOME),
          new NavEntry("revenue", "Thống kê doanh thu", "chart-no-axes-combined", PageId.REVENUE_STATS),
          new NavEntry("long_stock", "Báo cáo tồn kho lâu ngày", "package-search", PageId.LONG_STOCK_REPORT)
      );
    } else {
      return List.of(
          new NavEntry("home", "Trang chủ", "layout-dashboard", PageId.SALES_HOME),
          new NavEntry("customers", "Quản lý khách hàng", "users", PageId.CUSTOMERS),
          new NavEntry("invoice", "Lập hóa đơn bán lẻ", "receipt-text", PageId.RETAIL_INVOICE)
      );
    }
  }

  public static PageId getDefaultPageFor(LoaiQuyen loaiQuyen) {
    Objects.requireNonNull(loaiQuyen, "loaiQuyen");
    return (loaiQuyen == LoaiQuyen.NHAN_VIEN_QUAN_LY)
        ? PageId.MANAGER_HOME
        : PageId.SALES_HOME;
  }
}
