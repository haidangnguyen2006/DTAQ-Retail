package com.dtaq.retail.ui.shell;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.dtaq.retail.entity.LoaiQuyen;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Kiểm tra mô hình điều hướng NavigationModel")
class NavigationModelTest {

  @Test
  @DisplayName("Nhân viên bán hàng có đúng 3 mục menu và không có mục Thống kê")
  void testSalesNavEntries() {
    List<NavigationModel.NavEntry> entries = NavigationModel.getEntriesFor(LoaiQuyen.NHAN_VIEN_BAN_HANG);
    assertEquals(3, entries.size());
    assertEquals(PageId.SALES_HOME, entries.get(0).pageId());
    assertEquals(PageId.CUSTOMERS, entries.get(1).pageId());
    assertEquals(PageId.RETAIL_INVOICE, entries.get(2).pageId());

    for (NavigationModel.NavEntry entry : entries) {
      assertFalse(entry.label().equalsIgnoreCase("Thống kê"), "Menu NVBH không được chứa mục Thống kê");
    }

    assertEquals(PageId.SALES_HOME, NavigationModel.getDefaultPageFor(LoaiQuyen.NHAN_VIEN_BAN_HANG));
  }

  @Test
  @DisplayName("Nhân viên quản lý có đúng 3 mục menu theo Figma")
  void testManagerNavEntries() {
    List<NavigationModel.NavEntry> entries = NavigationModel.getEntriesFor(LoaiQuyen.NHAN_VIEN_QUAN_LY);
    assertEquals(3, entries.size());
    assertEquals(PageId.MANAGER_HOME, entries.get(0).pageId());
    assertEquals(PageId.REVENUE_STATS, entries.get(1).pageId());
    assertEquals(PageId.LONG_STOCK_REPORT, entries.get(2).pageId());

    assertEquals(PageId.MANAGER_HOME, NavigationModel.getDefaultPageFor(LoaiQuyen.NHAN_VIEN_QUAN_LY));
  }
}
