package com.dtaq.retail.ui.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import java.time.LocalDate;
import javax.swing.JPanel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests điểm cắm {@link NavContentFactory} — các panel Swing phải tạo được headless (không hiển thị
 * cửa sổ).
 */
class NavContentFactoryTest {

  /** Nhân viên bán hàng mẫu, đúng thứ tự 9 thành phần của record {@link NhanVien}. */
  private static final NhanVien NV_GIA =
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

  @ParameterizedTest
  @EnumSource(NavItem.class)
  void createReturnsContentForEveryItem(NavItem item) {
    JPanel result = NavContentFactory.create(item, NV_GIA);
    assertNotNull(result, "panel null cho: " + item.name());
    if (item == NavItem.TRANG_CHU) {
      assertInstanceOf(HomePanel.class, result);
    } else {
      PlaceholderPanel placeholder = assertInstanceOf(PlaceholderPanel.class, result);
      assertEquals("placeholderPanel." + item.name(), placeholder.getName());
    }
  }

  @Test
  void createRejectsNullItem() {
    assertThrows(NullPointerException.class, () -> NavContentFactory.create(null, NV_GIA));
  }

  @Test
  void createRejectsNullNhanVien() {
    assertThrows(
        NullPointerException.class, () -> NavContentFactory.create(NavItem.TRANG_CHU, null));
  }
}
