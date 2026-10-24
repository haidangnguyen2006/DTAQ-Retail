package com.dtaq.retail.ui.nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dtaq.retail.entity.LoaiQuyen;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Tests vai trò và tham chiếu tài liệu của {@link NavItem}. */
class NavItemTest {

  @Test
  void withRoleQuanLyReturnsExactOrder() {
    List<NavItem> items = NavItem.withRole(LoaiQuyen.NHAN_VIEN_QUAN_LY);
    assertEquals(
        List.of(
            NavItem.TRANG_CHU,
            NavItem.QUAN_LY_NHAN_VIEN,
            NavItem.THONG_KE_DOANH_THU,
            NavItem.THONG_KE_DOANH_SO_NV,
            NavItem.BAO_CAO_TON_KHO,
            NavItem.SAO_LUU_DU_LIEU,
            NavItem.PHUC_HOI_DU_LIEU),
        items);
  }

  @Test
  void withRoleBanHangReturnsExactOrder() {
    List<NavItem> items = NavItem.withRole(LoaiQuyen.NHAN_VIEN_BAN_HANG);
    assertEquals(
        List.of(
            NavItem.TRANG_CHU,
            NavItem.QUAN_LY_KHACH_HANG,
            NavItem.QUAN_LY_LOAI_SAN_PHAM,
            NavItem.QUAN_LY_SAN_PHAM,
            NavItem.LAP_HOA_DON_BAN_LE,
            NavItem.XU_LY_DOI_TRA_HANG),
        items);
  }

  @Test
  void isAllowedFollowsRole() {
    assertTrue(NavItem.TRANG_CHU.isAllowed(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    assertTrue(NavItem.TRANG_CHU.isAllowed(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    assertTrue(NavItem.QUAN_LY_NHAN_VIEN.isAllowed(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    assertFalse(NavItem.QUAN_LY_NHAN_VIEN.isAllowed(LoaiQuyen.NHAN_VIEN_BAN_HANG));
    assertFalse(NavItem.LAP_HOA_DON_BAN_LE.isAllowed(LoaiQuyen.NHAN_VIEN_QUAN_LY));
    assertTrue(NavItem.LAP_HOA_DON_BAN_LE.isAllowed(LoaiQuyen.NHAN_VIEN_BAN_HANG));
  }

  @Test
  void isAllowedRejectsNullRole() {
    assertThrows(NullPointerException.class, () -> NavItem.TRANG_CHU.isAllowed(null));
  }

  @Test
  void withRoleRejectsNullRole() {
    assertThrows(NullPointerException.class, () -> NavItem.withRole(null));
  }

  @Test
  void everyConstantHasNonNullBlankUniqueLabel() {
    List<NavItem> all = Arrays.asList(NavItem.values());
    Set<String> labels = new java.util.HashSet<>();
    for (NavItem item : all) {
      assertFalse(item.label().isBlank(), "label trống: " + item.name());
      assertFalse(item.sourceRef().isBlank(), "sourceRef trống: " + item.name());
      assertTrue(labels.add(item.label()), "label trùng: " + item.label());
      assertFalse(item.name().isBlank(), "name (key CardLayout) trống: " + item.name());
    }
    assertEquals(all.size(), labels.size());
  }

  @Test
  void enumHasExactlyTwelveConstantsInSidebarOrder() {
    List<String> names = Arrays.stream(NavItem.values()).map(Enum::name).toList();
    assertEquals(
        List.of(
            "TRANG_CHU",
            "QUAN_LY_NHAN_VIEN",
            "THONG_KE_DOANH_THU",
            "THONG_KE_DOANH_SO_NV",
            "BAO_CAO_TON_KHO",
            "SAO_LUU_DU_LIEU",
            "PHUC_HOI_DU_LIEU",
            "QUAN_LY_KHACH_HANG",
            "QUAN_LY_LOAI_SAN_PHAM",
            "QUAN_LY_SAN_PHAM",
            "LAP_HOA_DON_BAN_LE",
            "XU_LY_DOI_TRA_HANG"),
        names);
  }
}
