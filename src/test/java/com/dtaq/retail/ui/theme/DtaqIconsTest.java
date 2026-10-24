package com.dtaq.retail.ui.theme;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.util.List;
import javax.swing.Icon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Kiểm tra bộ biểu tượng DtaqIcons")
class DtaqIconsTest {

  private static final List<String> ALL_ICONS = List.of(
      "zap", "layout-dashboard", "chart-no-axes-combined", "package-search",
      "users", "receipt-text", "log-out", "download", "search", "chevron-down",
      "plus", "x", "info", "phone", "circle-check", "tv", "package",
      "shopping-cart", "calendar-days", "clock", "alert-circle", "user-round",
      "lock-keyhole", "chevron-left", "chevron-right", "check"
  );

  @Test
  @DisplayName("Tất cả 26 icon SVG phải nạp thành công từ classpath và tìm thấy tài nguyên")
  void testAllIconsFound() {
    for (String name : ALL_ICONS) {
      Icon icon = DtaqIcons.icon(name, 20, DtaqColors.PRIMARY);
      assertNotNull(icon, "Icon " + name + " không được null");
      assertTrue(icon instanceof FlatSVGIcon, "Icon " + name + " phải là FlatSVGIcon");
      FlatSVGIcon svgIcon = (FlatSVGIcon) icon;
      assertTrue(svgIcon.hasFound(), "Tài nguyên SVG của icon '" + name + "' phải tồn tại trong classpath");
    }
  }
}
