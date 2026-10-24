package com.dtaq.retail.ui;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.nav.NavContentFactory;
import com.dtaq.retail.ui.nav.NavItem;
import com.dtaq.retail.ui.nav.SidebarPanel;
import com.dtaq.retail.util.AppInfo;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.time.LocalDate;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import net.miginfocom.swing.MigLayout;

/**
 * Cửa sổ chính của ứng dụng — khung điều hướng shell gồm thanh bên trái
 * ({@link SidebarPanel}) và khu làm việc phải (top bar + thẻ nội dung theo
 * {@link NavItem} qua {@link NavContentFactory}, điều phối bằng {@link CardLayout}).
 */
public final class MainFrame extends JFrame {

  /** Chữ xám cho nhãn phụ (vai trò trong khối người dùng, nhãn ngày). */
  private static final Color GREY = new Color(0x6B7280);

  /** Nhân viên của phiên đăng nhập hiện tại (quyết định vai trò và các mục nav). */
  private final NhanVien nhanVien;

  /** Nhãn breadcrumb trên top bar, nội dung do {@link #selectSection(NavItem)} đặt. */
  private final JLabel breadcrumbLabel;

  /** Khu nội dung dạng thẻ, key mỗi thẻ là {@link NavItem#name()}. */
  private final JPanel content;

  /**
   * Builds the main window for the signed-in employee.
   *
   * @param nhanVien the employee owning the current session
   * @param onLogout callback invoked after the session is cleared from this window
   */
  public MainFrame(NhanVien nhanVien, Runnable onLogout) {
    super(AppInfo.APP_NAME);
    if (nhanVien == null) {
      throw new IllegalArgumentException("nhanVien must not be null");
    }
    if (onLogout == null) {
      throw new IllegalArgumentException("onLogout must not be null");
    }
    this.nhanVien = nhanVien;

    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    setLayout(new MigLayout("fill, insets 0", "[280!] [grow]", "[grow]"));

    JPanel sidebar = new SidebarPanel(nhanVien, this::selectSection);
    add(sidebar, "grow, push");

    JPanel right = new JPanel(new BorderLayout());
    add(right, "grow, push, wrap");

    JPanel topBar =
        new JPanel(new MigLayout("fillx, insets 16 24 16 24", "[][grow][grow][grow]", "[]"));
    topBar.setName("topBar");
    breadcrumbLabel = new JLabel();
    breadcrumbLabel.setName("breadcrumb");
    breadcrumbLabel.setFont(breadcrumbLabel.getFont().deriveFont(Font.BOLD, 16f));
    JLabel dateLabel = new JLabel(LocalDate.now().toString());
    dateLabel.setForeground(GREY);

    JPanel userBlock = new JPanel(new MigLayout("wrap 1, insets 0"));
    userBlock.setName("userBlock");
    JLabel hoTenLabel = new JLabel(nhanVien.hoTen());
    hoTenLabel.setFont(hoTenLabel.getFont().deriveFont(Font.BOLD));
    JLabel vaiTroLabel = new JLabel(nhanVien.moTaQuyen());
    vaiTroLabel.setForeground(GREY);
    userBlock.add(hoTenLabel);
    userBlock.add(vaiTroLabel);

    JButton logoutButton = new JButton("Đăng xuất");
    logoutButton.addActionListener(event -> confirmLogout(onLogout));

    topBar.add(breadcrumbLabel);
    topBar.add(dateLabel, "grow");
    topBar.add(userBlock, "grow");
    topBar.add(logoutButton, "grow");

    content = new JPanel(new CardLayout());
    content.setName("content");
    for (NavItem item : NavItem.withRole(nhanVien.loaiQuyen())) {
      content.add(NavContentFactory.create(item, nhanVien), item.name());
    }

    right.add(topBar, BorderLayout.NORTH);
    right.add(content, BorderLayout.CENTER);

    selectSection(NavItem.TRANG_CHU);

    pack();
    setMinimumSize(new Dimension(1180, 740));
    AppInfo.centerOnScreen(this);
  }

  /** Cập nhật breadcrumb (vai trò / tên mục) và hiển thị thẻ nội dung của mục được chọn. */
  private void selectSection(NavItem item) {
    breadcrumbLabel.setText(nhanVien.moTaQuyen() + " / " + item.label());
    ((CardLayout) content.getLayout()).show(content, item.name());
  }

  private void confirmLogout(Runnable onLogout) {
    int choice = JOptionPane.showConfirmDialog(
        this,
        "Bạn có chắc muốn đăng xuất?",
        "Xác nhận đăng xuất",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE);
    if (choice == JOptionPane.YES_OPTION) {
      dispose();
      onLogout.run();
    }
  }
}
