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
          new NavEntry("long_stock", "Báo cáo tồn kho lâu ngày", "package-search", PageId.LONG_STOCK_REPORT),
          new NavEntry("product_lines", "Quản lý dòng sản phẩm", "shopping-cart", PageId.PRODUCT_LINES),
          new NavEntry("defective_products", "Quản lý sản phẩm lỗi", "alert-circle", PageId.DEFECTIVE_PRODUCTS),
          new NavEntry("promotions", "Quản lý khuyến mãi", "zap", PageId.PROMOTIONS),
          new NavEntry("employees", "Quản lý nhân viên", "users", PageId.EMPLOYEES),
          new NavEntry("sales_by_product", "Thống kê doanh số theo sản phẩm", "chart-no-axes-combined", PageId.SALES_BY_PRODUCT),
          new NavEntry("sales_by_employee", "Thống kê doanh số theo nhân viên", "users", PageId.SALES_BY_EMPLOYEE),
          new NavEntry("return_approvals", "Phê duyệt yêu cầu đổi trả", "circle-check", PageId.RETURN_APPROVALS),
          new NavEntry("price_tax", "Cập nhật giá bán và thuế suất", "receipt-text", PageId.PRICE_TAX),
          new NavEntry("backup", "Sao lưu dữ liệu", "download", PageId.BACKUP),
          new NavEntry("restore", "Phục hồi dữ liệu", "clock", PageId.RESTORE)
      );
    } else {
      return List.of(
          new NavEntry("home", "Trang chủ", "layout-dashboard", PageId.SALES_HOME),
          new NavEntry("customers", "Quản lý khách hàng", "users", PageId.CUSTOMERS),
          new NavEntry("invoice", "Lập hóa đơn bán lẻ", "receipt-text", PageId.RETAIL_INVOICE),
          new NavEntry("product_lookup", "Tra cứu sản phẩm và tồn kho", "search", PageId.PRODUCT_LOOKUP),
          new NavEntry("customer_lookup", "Tra cứu khách hàng và giao dịch", "user-round", PageId.CUSTOMER_LOOKUP),
          new NavEntry("returns", "Xử lý đổi/trả hàng", "receipt-text", PageId.RETURNS),
          new NavEntry("products", "Quản lý sản phẩm", "package", PageId.PRODUCTS),
          new NavEntry("product_lines", "Quản lý dòng sản phẩm", "shopping-cart", PageId.PRODUCT_LINES),
          new NavEntry("categories", "Quản lý danh mục sản phẩm", "info", PageId.CATEGORIES),
          new NavEntry("defective_products", "Quản lý sản phẩm lỗi", "alert-circle", PageId.DEFECTIVE_PRODUCTS)
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
