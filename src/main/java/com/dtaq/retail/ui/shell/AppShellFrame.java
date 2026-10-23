package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.util.AppInfo;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Cửa sổ vỏ chính (App Shell Frame) của ứng dụng DTAQ.
 * Kết hợp Sidebar điều hướng theo vai trò, Topbar tiêu đề & phiên người dùng, và vùng nội dung chuyển trang bằng CardLayout.
 */
public class AppShellFrame extends JFrame {

  private final NhanVien nhanVien;
  private final Sidebar sidebar;
  private final Topbar topbar;
  private final JPanel contentCards;
  private final CardLayout cardLayout;
  private final Map<PageId, Page> registeredPages = new HashMap<>();

  private Page currentPage;

  public AppShellFrame(NhanVien nhanVien, Runnable onLogout) {
    super(AppInfo.APP_NAME);
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    // 1. Sidebar (WEST)
    sidebar = new Sidebar(nhanVien);
    sidebar.setOnNavigate(this::showPage);
    add(sidebar, BorderLayout.WEST);

    // 2. Right pane (Topbar NORTH + Content CENTER)
    JPanel rightContainer = new JPanel(new BorderLayout());
    rightContainer.setOpaque(false);

    topbar = new Topbar(nhanVien, onLogout);
    rightContainer.add(topbar, BorderLayout.NORTH);

    cardLayout = new CardLayout();
    contentCards = new JPanel(cardLayout);
    contentCards.setBackground(DtaqColors.BG_APP);

    JScrollPane scrollPane = new JScrollPane(contentCards);
    scrollPane.setBorder(null);
    scrollPane.setBackground(DtaqColors.BG_APP);
    scrollPane.getViewport().setBackground(DtaqColors.BG_APP);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    rightContainer.add(scrollPane, BorderLayout.CENTER);
    add(rightContainer, BorderLayout.CENTER);

    // Window dimensions & state
    setPreferredSize(new Dimension(1440, 960));
    setMinimumSize(new Dimension(1280, 760));
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    AppInfo.centerOnScreen(this);
  }

  public void registerPage(Page page) {
    Objects.requireNonNull(page, "page");
    registeredPages.put(page.id(), page);
    contentCards.add(page.view(), page.id().name());
  }

  public void showPage(PageId pageId) {
    Page targetPage = registeredPages.get(pageId);
    if (targetPage == null) {
      return;
    }

    if (currentPage != null) {
      currentPage.onHide();
    }

    cardLayout.show(contentCards, pageId.name());
    currentPage = targetPage;
    currentPage.onShow();

    sidebar.selectPage(pageId);
    topbar.setBreadcrumb(targetPage.section(), targetPage.title());
  }

  public NhanVien getNhanVien() {
    return nhanVien;
  }
}
