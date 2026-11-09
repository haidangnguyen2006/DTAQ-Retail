package com.dtaq.retail.ui.shell;

import com.dtaq.retail.entity.NhanVien;
import com.dtaq.retail.ui.component.ViewportPanel;
import com.dtaq.retail.ui.component.Responsive;
import com.dtaq.retail.ui.theme.DtaqColors;
import com.dtaq.retail.ui.theme.DtaqTheme;
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

  private static final int NARROW_WIDTH = 1280;
  private boolean sidebarChosenByUser;
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
    setIconImages(DtaqTheme.windowIcons());
    JPanel sidebarArea = new JPanel(new BorderLayout());
    sidebarArea.add(sidebar, BorderLayout.CENTER);
    JPanel resizeHandle = new JPanel();
    resizeHandle.setBackground(DtaqColors.BORDER);
    resizeHandle.setPreferredSize(new Dimension(5, 0));
    resizeHandle.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.E_RESIZE_CURSOR));
    resizeHandle.setToolTipText("Kéo để điều chỉnh độ rộng menu");
    java.awt.event.MouseAdapter resize = new java.awt.event.MouseAdapter() {
      private int startX;
      private int startWidth;

      @Override
      public void mousePressed(java.awt.event.MouseEvent e) {
        startX = e.getXOnScreen();
        startWidth = sidebar.getWidth();
      }

      @Override
      public void mouseDragged(java.awt.event.MouseEvent e) {
        if (sidebar.isCompact()) return;
        sidebarChosenByUser = true;
        sidebar.setExpandedWidth(Math.min(getWidth() - 640,
            startWidth + e.getXOnScreen() - startX));
      }
    };
    resizeHandle.addMouseListener(resize);
    resizeHandle.addMouseMotionListener(resize);
    sidebarArea.add(resizeHandle, BorderLayout.EAST);
    add(sidebarArea, BorderLayout.WEST);

    JPanel rightContainer = new JPanel(new BorderLayout());
    rightContainer.setOpaque(false);

    topbar = new Topbar(nhanVien, onLogout);
    topbar.setOnToggleSidebar(() -> {
      sidebarChosenByUser = true;
      sidebar.setCompact(!sidebar.isCompact());
    });
    rightContainer.add(topbar, BorderLayout.NORTH);

    cardLayout = new CardLayout();
    contentCards = new JPanel(cardLayout);
    contentCards.setBackground(DtaqColors.BG_APP);
    rightContainer.add(contentCards, BorderLayout.CENTER);
    add(rightContainer, BorderLayout.CENTER);

    Responsive.fitWindow(this, new Dimension(1440, 900), new Dimension(1024, 600));
    // Cửa sổ hẹp thì tự thu sidebar thành dải icon; người dùng bấm nút thì giữ lựa chọn của họ.
    addComponentListener(new java.awt.event.ComponentAdapter() {
      private boolean wasNarrow;

      @Override
      public void componentResized(java.awt.event.ComponentEvent e) {
        boolean narrow = getWidth() < NARROW_WIDTH;
        if (narrow != wasNarrow) {
          wasNarrow = narrow;
          sidebarChosenByUser = false;
        }
        if (!sidebarChosenByUser && sidebar.isCompact() != narrow) {
          sidebar.setCompact(narrow);
        }
      }
    });
    setExtendedState(JFrame.MAXIMIZED_BOTH);

  }

  public void registerPage(Page page) {
    Objects.requireNonNull(page, "page");
    registeredPages.put(page.id(), page);
    contentCards.add(wrap(page), page.id().name());
  }

  /** Trang tự giãn chỉ cuộn ngoài khi chiều cao tối thiểu vượt vùng làm việc. */
  private static JComponent wrap(Page page) {
    JComponent content = page.scrollable() ? centered(page.view()) : page.view();
    ViewportPanel viewport = new ViewportPanel(content) {
      @Override
      public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        if (!page.scrollable() && getParent() instanceof javax.swing.JViewport parent) {
          size.height = Math.max(content.getMinimumSize().height, parent.getHeight());
        }
        return size;
      }
    };
    JScrollPane scroll = new JScrollPane(viewport);
    scroll.setBorder(null);
    scroll.getViewport().setBackground(DtaqColors.BG_APP);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    return scroll;
  }

  /** Căn giữa nội dung trang và giới hạn bề rộng tối đa để không bị kéo giãn quá mức trên màn lớn. */
  private static JComponent centered(JComponent view) {
    JPanel holder = new JPanel(new net.miginfocom.swing.MigLayout("fillx, insets 0", "[0:pref,grow,center]", "[]"));
    holder.setOpaque(false);
    holder.add(view, "growx, wmax " + com.dtaq.retail.ui.theme.DtaqSpacing.CONTENT_MAX_W);
    return holder;
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
