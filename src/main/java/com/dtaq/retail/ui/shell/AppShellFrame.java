package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.ViewportPanel;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.util.AppInfo;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;

/**
 * Cửa sổ vỏ chính (App Shell Frame) của ứng dụng DTAQ.
 * Kết hợp Sidebar điều hướng theo vai trò, Topbar tiêu đề & phiên người dùng, và vùng nội dung
 * chuyển trang bằng CardLayout. Mỗi trang có thanh cuộn riêng (trừ trang tự giãn) và phím tắt riêng.
 */
public class AppShellFrame extends JFrame {

  private static final String SHORTCUT_PREFIX = "dtaq.page.";

  private final NhanVien nhanVien;
  private final Sidebar sidebar;
  private final Topbar topbar;
  private final JPanel contentCards;
  private final CardLayout cardLayout;
  private final Map<PageId, Page> registeredPages = new HashMap<>();

  private Page currentPage;
  private final List<KeyStroke> installedKeys = new ArrayList<>();
  private final List<Object> installedActionKeys = new ArrayList<>();

  public AppShellFrame(NhanVien nhanVien, Runnable onLogout) {
    super(AppInfo.APP_NAME);
    this.nhanVien = Objects.requireNonNull(nhanVien, "nhanVien");

    setDefaultCloseOperation(EXIT_ON_CLOSE);
    setLayout(new BorderLayout());

    sidebar = new Sidebar(nhanVien);
    sidebar.setOnNavigate(this::showPage);
    add(sidebar, BorderLayout.WEST);

    JPanel rightContainer = new JPanel(new BorderLayout());
    rightContainer.setOpaque(false);

    topbar = new Topbar(nhanVien, onLogout);
    rightContainer.add(topbar, BorderLayout.NORTH);

    cardLayout = new CardLayout();
    contentCards = new JPanel(cardLayout);
    contentCards.setBackground(DtaqColors.BG_APP);
    rightContainer.add(contentCards, BorderLayout.CENTER);
    add(rightContainer, BorderLayout.CENTER);

    setPreferredSize(new Dimension(1440, 960));
    setMinimumSize(new Dimension(1100, 680));
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    AppInfo.centerOnScreen(this);
  }

  public void registerPage(Page page) {
    Objects.requireNonNull(page, "page");
    registeredPages.put(page.id(), page);
    contentCards.add(wrap(page), page.id().name());
  }

  /** Trang cuộn được thì có {@link JScrollPane} riêng; trang tự giãn thì đặt thẳng vào khung. */
  private static JComponent wrap(Page page) {
    if (!page.scrollable()) {
      return page.view();
    }
    JScrollPane scroll = new JScrollPane(new ViewportPanel(page.view()));
    scroll.setBorder(null);
    scroll.getViewport().setBackground(DtaqColors.BG_APP);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    return scroll;
  }

  public void showPage(PageId pageId) {
    Page targetPage = registeredPages.get(pageId);
    if (targetPage == null) {
      return;
    }

    if (currentPage != null) {
      currentPage.onHide();
    }
    uninstallPageShortcuts();

    cardLayout.show(contentCards, pageId.name());
    currentPage = targetPage;
    installPageShortcuts(targetPage);
    currentPage.onShow();

    sidebar.selectPage(pageId);
    topbar.setBreadcrumb(targetPage.section(), targetPage.title());
  }

  /** Chép phím tắt của trang vào root pane (có tiền tố riêng) để gỡ sạch khi chuyển trang. */
  private void installPageShortcuts(Page page) {
    InputMap pageInput = new InputMap();
    ActionMap pageActions = new ActionMap();
    page.installShortcuts(pageInput, pageActions);

    InputMap rootInput = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap rootActions = getRootPane().getActionMap();
    KeyStroke[] keys = pageInput.keys();
    if (keys != null) {
      for (KeyStroke key : keys) {
        Object name = pageInput.get(key);
        rootInput.put(key, SHORTCUT_PREFIX + name);
        installedKeys.add(key);
      }
    }
    Object[] actionKeys = pageActions.keys();
    if (actionKeys != null) {
      for (Object name : actionKeys) {
        Object key = SHORTCUT_PREFIX + name;
        rootActions.put(key, pageActions.get(name));
        installedActionKeys.add(key);
      }
    }
  }

  private void uninstallPageShortcuts() {
    InputMap rootInput = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionMap rootActions = getRootPane().getActionMap();
    installedKeys.forEach(rootInput::remove);
    installedActionKeys.forEach(rootActions::remove);
    installedKeys.clear();
    installedActionKeys.clear();
  }

  public NhanVien getNhanVien() {
    return nhanVien;
  }
}
