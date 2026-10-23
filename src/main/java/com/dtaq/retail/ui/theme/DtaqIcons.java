package com.dtaq.retail.ui.theme;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import java.awt.Color;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.Icon;

/**
 * Kho cung cấp biểu tượng (Lucide SVG) dùng chung cho toàn bộ giao diện DTAQ.
 * Hỗ trợ tỉ lệ co giãn HiDPI và tô màu tự do theo design tokens.
 */
public final class DtaqIcons {

  private static final String ICONS_PACKAGE = "com/dtaq/retail/ui/theme/icons/";
  private static final ConcurrentMap<String, FlatSVGIcon> CACHE = new ConcurrentHashMap<>();

  private DtaqIcons() {
  }

  /**
   * Tạo hoặc lấy biểu tượng SVG với kích thước và màu sắc chỉ định.
   *
   * @param name tên biểu tượng (không kèm phần mở rộng .svg); không được {@code null}
   * @param size kích thước (chiều rộng = chiều cao tính bằng pixel logic)
   * @param color màu sắc cần áp dụng; nếu {@code null} thì giữ màu gốc
   * @return biểu tượng {@link Icon} sẵn sàng hiển thị
   */
  public static Icon icon(String name, int size, Color color) {
    Objects.requireNonNull(name, "name");
    String resourcePath = ICONS_PACKAGE + name + ".svg";
    FlatSVGIcon base = CACHE.computeIfAbsent(
        resourcePath + "@" + size,
        key -> new FlatSVGIcon(resourcePath, size, size));

    if (color == null) {
      return base;
    }

    FlatSVGIcon tinted = base.derive(size, size);
    tinted.setColorFilter(new FlatSVGIcon.ColorFilter(c -> color));
    return tinted;
  }

  /**
   * Lấy biểu tượng với kích thước chỉ định và màu mặc định {@link DtaqColors#TEXT_MUTED}.
   */
  public static Icon icon(String name, int size) {
    return icon(name, size, DtaqColors.TEXT_MUTED);
  }

  /**
   * Lấy biểu tượng với kích thước chuẩn 18px và màu mặc định.
   */
  public static Icon icon(String name) {
    return icon(name, 18, DtaqColors.TEXT_MUTED);
  }
}
