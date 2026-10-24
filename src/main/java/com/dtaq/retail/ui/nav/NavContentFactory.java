package com.dtaq.retail.ui.nav;

import com.dtaq.retail.entity.NhanVien;
import java.util.Objects;
import javax.swing.JPanel;

/**
 * Điểm cắm duy nhất để gắn panel tính năng vào khung điều hướng của màn hình chính.
 *
 * <p>Tạo (không hiển thị) panel nội dung cho từng {@link NavItem} theo vai trò nhân viên: mục
 * {@link NavItem#TRANG_CHU} mở {@link HomePanel}, các mục còn lại mở {@link PlaceholderPanel} cho
 * đến khi tính năng tương ứng được hiện thực.
 */
public final class NavContentFactory {

  private NavContentFactory() {
  }

  /* ============================================================
   * ĐIỂM CẮM TÍNH NĂNG — mỗi member gắn panel của mình vào đây:
   * thay nhánh `default -> new PlaceholderPanel(item);` bên dưới
   * bằng nhánh riêng trước khi thay default, ví dụ:
   *
   *   case LAP_HOA_DON_BAN_LE -> new LapHoaDonPanel(nhanVien);
   *
   * Nguyên tắc:
   * - KHÔNG xóa / đổi nhánh TRANG_CHU (HomePanel) và các requireNonNull.
   * - Chỉ thêm nhánh case cho NavItem của mình; không sửa nhánh của người khác.
   * - Panel mới cần constructor nhận tham số phù hợp, ví dụ
   *   `new LapHoaDonPanel(nhanVien)`; giữ nguyên chữ ký create(...) — MainFrame (U3) phụ thuộc nó.
   * ============================================================ */
  public static JPanel create(NavItem item, NhanVien nhanVien) {
    Objects.requireNonNull(item, "item");
    Objects.requireNonNull(nhanVien, "nhanVien");
    return switch (item) {
      case TRANG_CHU -> new HomePanel(nhanVien);
      default -> new PlaceholderPanel(item);
    };
  }
}
