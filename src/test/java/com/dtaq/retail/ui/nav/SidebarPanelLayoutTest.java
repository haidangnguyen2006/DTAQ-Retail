package com.dtaq.retail.ui.nav;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import java.time.LocalDate;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Test dựng panel headless (không hiển thị cửa sổ): MigLayout phân tích chuỗi layout ngay trong
 * constructor của {@link SidebarPanel}, nên khởi tạo thành công chứng minh các constraint hợp lệ.
 *
 * <p>Regression cho lỗi runtime {@code IllegalArgumentException: Illegal constraint: 'growx' for
 * input "x"} — constraint ngoài layout (cột) phải là ràng buộc một chiều (ví dụ {@code grow});
 * {@code growx} chỉ hợp lệ ở component constraint.
 */
class SidebarPanelLayoutTest {

  /** Nhân viên bán hàng mẫu, đúng thứ tự 9 thành phần của record {@link NhanVien}. */
  private static final NhanVien NV_BAN_HANG =
      new NhanVien(
          "NVBANHANG-2026-001",
          "Lê Thu Hà",
          "0900000000",
          "lethuha@dtaq.vn",
          LocalDate.of(2025, 1, 10),
          "lethuha",
          "$2a$10$seed",
          LoaiQuyen.NHAN_VIEN_BAN_HANG,
          true);

  /** Nhân viên quản lý mẫu, cùng bộ tham số thử vai trò còn lại. */
  private static final NhanVien NV_QUAN_LY =
      new NhanVien(
          "NVQUANLY-2026-001",
          "Trần Quang Minh",
          "0901111111",
          "quangminh@dtaq.vn",
          LocalDate.of(2024, 6, 1),
          "quangminh",
          "$2a$10$seed",
          LoaiQuyen.NHAN_VIEN_QUAN_LY,
          true);

  /** Callback rỗng: chỉ cần SidebarPanel dựng được, không cần điều hướng thật. */
  private static final Consumer<NavItem> NO_NAVIGATE = item -> { };

  @Test
  void forSalesRole() {
    assertDoesNotThrow(() -> new SidebarPanel(NV_BAN_HANG, NO_NAVIGATE));
  }

  @Test
  void forManagerRole() {
    assertDoesNotThrow(() -> new SidebarPanel(NV_QUAN_LY, NO_NAVIGATE));
  }
}
