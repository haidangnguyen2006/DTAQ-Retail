package com.dtaq.retail.ui.nav;

import com.dtaq.retail.entity.LoaiQuyen;
import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.util.AppInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Thanh điều hướng bên trái của màn hình chính ("sidebar") theo prototype: khối thương hiệu DTAQ,
 * nhãn "KHÔNG GIAN LÀM VIỆC", danh sách mục theo vai trò ({@link NavItem#withRole(LoaiQuyen)
 * NavItem.withRole}) và footer phiên bản.
 *
 * <p>Trạng thái active đổi màu nền/chữ hàng loạt thông qua {@link #setActive(JButton)}; sau khi
 * dựng xong, mục {@link NavItem#TRANG_CHU} là mục active mặc định.
 */
public final class SidebarPanel extends JPanel {

  /** Màu thương hiệu teal (giống LoginFrame). */
  private static final Color BRAND = new Color(0x0F766E);

  /** Nền mục active. */
  private static final Color ACTIVE_BACKGROUND = new Color(0xDCF5F2);

  /** Nền trong suốt của mục mặc định. */
  private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

  /** Chữ xám đậm của mục mặc định. */
  private static final Color GREY_TEXT = new Color(0x374151);

  /** Chữ xám cho nhãn phụ (footer, nhãn mục). */
  private static final Color GREY = new Color(0x6B7280);

  /** Các nút nav theo đúng thứ tự {@link NavItem#withRole(LoaiQuyen) NavItem.withRole}. */
  private final List<JButton> navButtons;

  /** Nút đang active; {@code null} khi chưa có mục nào được chọn. */
  private JButton activeButton;

  /**
   * Khởi tạo sidebar cho một nhân viên.
   *
   * @param nhanVien nhân viên đang đăng nhập, quyết định vai trò và các mục hiển thị; không được
   *     {@code null}
   * @param onNavigate hành động khi chọn một mục; không được {@code null}
   */
  public SidebarPanel(NhanVien nhanVien, Consumer<NavItem> onNavigate) {
    super(new BorderLayout());
    Objects.requireNonNull(nhanVien, "nhanVien");
    Objects.requireNonNull(onNavigate, "onNavigate");
    setName("sidebarPanel");

    JPanel center = new JPanel(new MigLayout("fillx, wrap 1, insets 0", "[grow]", "[]"));
    center.add(brandBlock(nhanVien), "growx, wrap");
    center.add(sectionLabel(), "growx, wrap");

    navButtons = new ArrayList<>();
    for (NavItem item : NavItem.withRole(nhanVien.loaiQuyen())) {
      JButton button = navButton(item, onNavigate);
      navButtons.add(button);
      center.add(button, "growx, alignx left");
    }

    add(center, BorderLayout.CENTER);
    add(footer(nhanVien), BorderLayout.SOUTH);
    setActive(navButtons.getFirst());
  }

  /** Khối thương hiệu nền teal, chữ trắng: "DTAQ" + dòng phụ theo vai trò. */
  private static JPanel brandBlock(NhanVien nhanVien) {
    JPanel brand = new JPanel(new MigLayout("fill, wrap 1, insets 24", "", "[]"));
    brand.setBackground(BRAND);
    brand.setName("brandBlock");

    JLabel title = new JLabel("DTAQ");
    title.setForeground(Color.WHITE);
    title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));

    String subtitle = switch (nhanVien.loaiQuyen()) {
      case NHAN_VIEN_BAN_HANG -> "ĐIỆN MÁY";
      case NHAN_VIEN_QUAN_LY -> "QUẢN LÝ SIÊU THỊ ĐIỆN MÁY";
    };
    JLabel subtitleLabel = new JLabel(subtitle);
    subtitleLabel.setForeground(Color.WHITE);

    brand.add(title, "wrap");
    brand.add(subtitleLabel);
    return brand;
  }

  /** Nhãn mục nhỏ "KHÔNG GIAN LÀM VIỆC". */
  private static JLabel sectionLabel() {
    JLabel label = new JLabel("KHÔNG GIAN LÀM VIỆC");
    label.setForeground(GREY);
    label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
    return label;
  }

  /** Nút nav flat: active = nền nhạt + chữ teal; mặc định = nền trong suốt + chữ xám đậm. */
  private JButton navButton(NavItem item, Consumer<NavItem> onNavigate) {
    JButton button = new JButton(item.label());
    button.setName("nav." + item.name());
    button.putClientProperty("JButton.buttonType", "toolBarButton");
    button.setHorizontalAlignment(JButton.LEFT);
    button.addActionListener(event -> {
      setActive(button);
      onNavigate.accept(item);
    });
    applyStyle(button, false);
    return button;
  }

  /** Đặt nút thành mục active; giữ nguyên nếu nút đang active. */
  private void setActive(JButton button) {
    if (button == activeButton) {
      return;
    }
    activeButton = button;
    for (JButton nav : navButtons) {
      applyStyle(nav, nav == button);
    }
  }

  /** Áp màu nền/chữ + đậm theo trạng thái active của nút. */
  private static void applyStyle(JButton button, boolean active) {
    if (active) {
      button.setBackground(ACTIVE_BACKGROUND);
      button.setForeground(BRAND);
      button.setFont(button.getFont().deriveFont(Font.BOLD));
    } else {
      button.setBackground(TRANSPARENT);
      button.setForeground(GREY_TEXT);
      button.setFont(button.getFont().deriveFont(Font.PLAIN));
    }
    button.setOpaque(true);
    button.setFocusPainted(false);
  }

  /** Footer: vai trò + tên ứng dụng, phiên bản. */
  private static JPanel footer(NhanVien nhanVien) {
    JPanel footer = new JPanel(new MigLayout("fillx, wrap 1, insets 16 24 16 24", "[grow]", "[]"));
    footer.setName("sidebarFooter");

    JLabel role = new JLabel("DTAQ · " + nhanVien.moTaQuyen());
    JLabel version = new JLabel("Nội bộ DTAQ · Phiên bản " + AppInfo.version());
    version.setForeground(GREY);

    footer.add(role, "wrap");
    footer.add(version);
    return footer;
  }
}
